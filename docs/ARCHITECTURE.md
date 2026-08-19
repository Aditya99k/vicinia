# Vicinia — Architecture

Status: APPROVED. All decisions in §12 (Decision Log) are locked. Build order lives in `BUILD_TRACKER.md`; infra/hosting specifics live in `DEPLOYMENT.md`. Full rationale for each decision is preserved here — don't relitigate in the tracker, update this file (and the matching ADR) instead.

## 1. Product Scope

Vicinia is a hyperlocal, multi-merchant commerce platform. Differentiator vs. a standard e-commerce clone: independent local merchants self-onboard, run their own store (catalog listing, price, stock, hours), and fulfill orders placed against *their* store specifically — not a platform-wide warehouse.

V1 goal: prove the full loop — merchant onboards → customer discovers a nearby store → places an order from that one store → payment → merchant prepares → delivery partner delivers → merchant gets settled. Analytics, split orders, staff accounts, and real payouts are explicitly out of scope for V1.

## 2. Actors

| Role | Build in V1? | Reasoning |
|---|---|---|
| CUSTOMER | Yes | Core demand side. |
| MERCHANT (owner) | Yes | Core supply side. One account = one store owner. |
| DELIVERY_PARTNER | Yes | Core fulfillment side. |
| ADMIN | Yes | Platform operator: approves merchants, manages catalog/coupons, visibility into orders/payments. |
| Merchant staff | No | Additive later — `Merchant` gains a `staff[]` list with scoped permissions without changing any other service's contract. No V1 payoff, defer. |
| Support agent | No, as a service | Modeled as a **permission scope inside auth-service's RBAC** (read orders/payments, initiate refund, can't approve merchants/coupons) — not a new actor or service. |
| Super admin | No | Only matters once there are multiple admin accounts needing control over who creates other admins. |

**Design consequence:** build the RBAC model (role + permission enum, not a role string) from day one, even though V1 ships only 4 roles. That's what makes adding `SUPPORT_AGENT` or `MERCHANT_STAFF` later a config change, not a migration.

## 3. Feature List — V1 Cut

**Must have for a working checkout loop:** auth+JWT+refresh, merchant onboarding+approval, global catalog, merchant listings (price/stock), single-merchant cart, coupons, order placement, wallet + Razorpay payment, order status tracking, merchant accept/prepare, delivery assignment+tracking, admin visibility.

**Deferred past V1:** reviews/ratings, settlement automation (simulated/manual first), merchant analytics dashboards, refund automation (manual admin refund first), notification service beyond basic email/log.

## 4. Core Business Requirements

### 4.1 Merchant Onboarding — State Machine

```
PENDING_REVIEW → APPROVED → ONBOARDING → LIVE
              ↓
           REJECTED (terminal)

LIVE → SUSPENDED (admin-initiated, reversible → LIVE)
LIVE → TEMP_CLOSED (merchant-initiated, reversible → LIVE)
LIVE / SUSPENDED / TEMP_CLOSED → PERMANENTLY_CLOSED (terminal)
```

- `UNDER_REVIEW` was considered and **cut** — no behavioral difference from `PENDING_REVIEW`, cosmetic-only distinction (Decision Log #10).
- `ONBOARDING`: approved, but store isn't visible to customers yet — merchant must add ≥1 product listing + business hours before self-transitioning to `LIVE`. Prevents empty stores surfacing in search.
- `SUSPENDED` vs `TEMP_CLOSED`: same customer-facing effect (store hidden) but different actor and different reversal path — kept distinct so admins don't have to guess why a store went dark.

### 4.2 Product ↔ Merchant Modeling

Two-tier model, deliberately **not** "one product belongs to one merchant":

```
Product (catalog-service, global, canonical)
  productId, name, brand, category, images, attributes{}

MerchantListing (inventory-service, per merchant-product pair)
  merchantId, productId, price, stock, unit, isActive
```

- `Product` = "what is this item" — shared across merchants, read-heavy, cacheable, rarely changes.
- `MerchantListing` = "who sells it, for how much, how many" — merchant-specific, write-heavy, needs strong consistency.

This split is *why* `catalog-service` and `inventory-service` are separate services with different databases (§6) — genuinely different read/write and consistency profiles.

Search flow: customer searches "milk 1L" → catalog-service resolves matching `productId`s → inventory-service returns listings for those products within delivery radius, sorted by distance/price → customer picks a listing, pinning their cart to that merchant.

### 4.3 Order Fulfillment — Single Merchant per Order

**Decision: one merchant per order. No order-splitting in V1.**

Splitting multiplies almost every downstream concern: multiple deliveries or a merged route, partial payment capture per merchant, partial cancellation, multiple settlement records, multiple prep-time estimates to reconcile into one ETA. Real hyperlocal apps mostly force single-store carts too.

Implementation consequence: `cart-service` enforces one `merchantId` per cart. Adding an item from a different merchant either replaces the cart or prompts the user to clear it. An order therefore always has exactly one `merchantId` — never a runtime decision.

### 4.4 Inventory Concurrency — Postgres, Not Redis Locks

**Decision: PostgreSQL row-level atomic updates. No distributed lock service.**

```sql
UPDATE merchant_listing
SET available_stock = available_stock - :qty,
    reserved_stock   = reserved_stock + :qty
WHERE listing_id = :id AND available_stock >= :qty;
-- rows_affected == 0 → insufficient stock, reject reservation
```

Postgres's row lock during this statement is sufficient. A Redis distributed lock adds a new failure mode (lock unavailable, not released on crash, TTL mismatch) for zero correctness gain over what the database already guarantees. Redis *would* make sense for high-QPS "is this in stock" display reads — not for the write-path reservation itself.

**Reservation lifecycle:**

```
RESERVE (order created)    → available -= qty, reserved += qty
CONFIRM (payment success)  → reserved  -= qty   (stock permanently consumed)
RELEASE (failure/cancel/timeout) → available += qty, reserved -= qty
```

**Reservation timeout:** don't rely on Redis key-expiry events (opt-in, not durable, easy to miss under load/restart). Instead: every reservation row stores `reserved_at`; a scheduled job (every 1–2 min) finds reservations older than N minutes still `PAYMENT_PENDING` and releases them. Boring, reliable, driven by the database, survives restarts.

### 4.5 Order Lifecycle — State Ownership

```
CREATED → PAYMENT_PENDING → CONFIRMED → MERCHANT_ACCEPTED → PREPARING
        → READY_FOR_PICKUP → DELIVERY_ASSIGNED → OUT_FOR_DELIVERY → DELIVERED

Branches:
  PAYMENT_PENDING → PAYMENT_FAILED
  CREATED/CONFIRMED/PREPARING → CANCELLED → REFUND_PENDING → REFUNDED
  MERCHANT_ACCEPTED-eligible → MERCHANT_REJECTED → (refund path)
```

**Key decision:** `order-service` owns the single canonical `status` field for the entire lifecycle, even though transitions are triggered by three different services (order, merchant, delivery). Merchant-service and delivery-service manage their own internal task state, but publish events (`merchant.accepted`, `order.ready`, `delivery.assigned`, `delivery.delivered`) that order-service consumes to update `order.status`.

Why not live API composition (order + merchant + delivery) on every "track my order" read? That's 3 network calls with 3 failure modes per page load, for data that doesn't need millisecond freshness. Mirroring status via Kafka trades a few hundred ms of eventual consistency for a single fast query — clearly worth it here.

### 4.6 Payment Failure & Recovery

Standard idempotent event-consumption, not a special case:

1. Razorpay webhook handler in `payment-service` is idempotent on `razorpay_payment_id` (unique DB constraint — duplicate delivery hits a constraint violation and is dropped/ack'd).
2. `payment.success` is published to Kafka only **after** the transaction row is durably committed.
3. `order-service`'s consumer is idempotent on `orderId + target status`: if the order is already `CONFIRMED`, the event is a no-op. Kafka's at-least-once delivery + consumer replay-from-offset means a crashed order-service simply re-reads the event on restart — nothing is lost, re-processing is safe.

This is the general idempotency pattern reused everywhere (§9) — flagged here specifically because payment is where getting it wrong costs money.

### 4.7 Merchant Settlement

```
Customer pays ₹500
  Platform commission (5%) = ₹25
  Merchant payable         = ₹475
```

Triggered on `ORDER_DELIVERED` — deliberately **not** on `CONFIRMED`, so cancellations/refunds between confirm and delivery don't require clawing back a settlement already created.

1. Create `SettlementEntry(orderId, merchantId, gross, commission, net, status=PENDING)`.
2. A scheduled batch job aggregates a merchant's `PENDING` entries on a cadence (e.g. daily) into a `Payout(merchantId, totalAmount, entries[], status)`.
3. Payout status: `PENDING → PROCESSING → PAID | FAILED`. `PAID` is simulated in V1 (no real bank integration) — the state machine and ledger are real, which is the part that demonstrates the skill.

## 5. Technology Stack Notes

- **Java 21 + virtual threads**: worth enabling for I/O-bound services (order-service makes several blocking Feign calls per request) — cheap win, no architecture change, just a config flag.
- **Resilience4j scope**: see §8 — don't wrap every call, only ones with an identified failure mode.

## 6. Database Strategy — Per Service

| Service | Database | Why |
|---|---|---|
| auth-service | PostgreSQL | Structured credentials, unique constraints on email/phone, small relational role/permission tables. |
| user-service | PostgreSQL | User → many addresses is a textbook relational shape. |
| merchant-service | PostgreSQL | Onboarding is a strict state machine + relational fields; approval workflow benefits from transactional guarantees. |
| catalog-service | MongoDB | Variable attributes per category, read-heavy, cacheable, no cross-document transactions needed. |
| **inventory-service** | **PostgreSQL** | Needs atomic conditional updates + row-level locking to prevent overselling (§4.4). The single strongest "must be relational" case in the system. |
| cart-service | Redis | Ephemeral, TTL-based, no durability beyond a session. |
| coupon-service | PostgreSQL | Usage-limit enforcement is an atomic-increment-with-check problem, same family as inventory. |
| order-service | PostgreSQL | Most transactionally critical entity; relational integrity between order/items/status history matters. |
| payment-service | PostgreSQL | Financial data must be ACID; idempotency depends on unique constraints Postgres enforces natively. |
| delivery-service | PostgreSQL (task/state) + Redis (live GPS) | Task lifecycle is a state machine → relational. Live location is high-frequency/ephemeral/geo-queried → Redis `GEOADD`/`GEORADIUS`. |
| notification-service | MongoDB | Unstructured logs of what was sent, high write volume, no relational needs. |
| review-service | MongoDB | Flexible schema, read-heavy, no cross-entity transactions. |
| settlement-service | PostgreSQL | Financial ledger — must be exact and auditable. |

**Rule of thumb:** Postgres for anything that is a state machine, involves money, or needs an atomic conditional update under concurrency. Mongo for read-heavy, flexible-schema, low-consistency-requirement content.

## 7. Microservice Boundaries

```
config-server   discovery-server   api-gateway
auth-service    user-service       merchant-service
catalog-service inventory-service  cart-service
coupon-service  order-service      payment-service
delivery-service notification-service review-service
settlement-service
```

16 domain services + 3 infra services = 19 total.

- **catalog-service vs inventory-service** — keep split. Different DBs, read/write ratios, consistency needs. Clearest "genuinely different bounded contexts" case in the system.
- **merchant-service vs catalog-service** — keep split. A merchant "listing a product" either (a) attaches a listing to an existing catalog product (common case), or (b) requests a new catalog product be created (rarer, may need light moderation). Either way, merchant-service never owns catalog data — only merchant identity, store profile, and onboarding state.
- **settlement-service vs payment-service** — keep split, but lower priority: different cadence (real-time capture vs. batch payout), different data shape, different future integration surface. Nothing else in the checkout path depends on it, so it's built last.
- **No service for merchant staff / support agent** — RBAC concern inside auth-service, not a bounded context.

### Per-service ownership / dependencies / events

| Service | Owns | Sync deps (REST) | Kafka produced | Kafka consumed |
|---|---|---|---|---|
| auth-service | credentials, refresh tokens, RBAC roles | — | `user.registered`, `user.deleted` | — |
| user-service | profiles, addresses | — | — | `user.registered` |
| merchant-service | merchant entity, onboarding docs, store profile | — | `merchant.approved`, `merchant.suspended` | — |
| catalog-service | global product catalog, categories | — | `product.created` | — |
| inventory-service | merchant listings: price, stock, reservations | catalog-service (validate productId) | `inventory.low`, `inventory.out` | `product.created` |
| cart-service | active cart (Redis only) | catalog/inventory (price+availability), coupon (validate) | — | `order.confirmed` (clear cart) |
| coupon-service | coupons, usage records | — | — | — |
| order-service | order aggregate + canonical status | cart, inventory (reserve), payment (pay) | `order.created`, `order.confirmed`, `order.cancelled` | `payment.success`, `payment.failed`, `merchant.accepted`, `delivery.delivered` |
| payment-service | transactions, wallet balance | — | `payment.success`, `payment.failed` | `order.cancelled` (trigger refund) |
| delivery-service | delivery tasks, partner status/location | — | `delivery.assigned`, `delivery.delivered` | `order.confirmed` |
| notification-service | notification log | — | — | `user.registered`, `order.confirmed`, `payment.failed`, `inventory.low` |
| review-service | reviews | order-service (verify purchase) | — | — |
| settlement-service | settlement ledger, payouts | — | `settlement.completed` | `order.delivered` |

## 8. REST vs Kafka — Communication Matrix

**Rule:** if the caller needs the result to decide what to do next in the same request, it's REST. If the caller just needs to eventually know something happened, it's Kafka.

| Producer | Consumer | Mechanism | Reason |
|---|---|---|---|
| Gateway | any service | REST | Synchronous client request, needs a response now. |
| order-service | cart-service | REST | Needs current cart contents now to build the order. |
| order-service | inventory-service | REST | Reservation must succeed/fail before order creation — critical synchronous path. |
| order-service | payment-service | REST | Wallet debit result determines whether order proceeds immediately. |
| payment-service | order-service | Kafka | Razorpay confirmation is async by nature (webhook, arbitrary delay). |
| merchant-service | order-service | Kafka | Merchant accept/reject is human-paced (seconds to minutes). |
| delivery-service | order-service | Kafka | Delivery status changes over minutes/hours; order-service just mirrors it. |
| inventory-service | notification-service | Kafka | Low-stock alert is fire-and-forget. |
| merchant-service | catalog-service | REST (occasional) | Only if validating a productId exists before listing creation — low volume. |
| order-service | notification-service | Kafka | "Send confirmation email/SMS" should never block order placement. |
| * | settlement-service | Kafka | Settlement is inherently batch/deferred, never a sync checkout dependency. |

Inventory reservation and payment-initiation are REST *despite* being "state changes," because order-service's own logic branches on their result immediately. The crash-recovery case is handled by Kafka on the confirmation side (§4.6), not the initiation side — don't conflate the two.

## 9. Kafka Architecture

**Topic granularity: one topic per aggregate, not per event type** — `order-events`, `payment-events`, `inventory-events`, `delivery-events`, `merchant-events`, `user-events`, each carrying an `eventType` field. Keeps ordering guarantees simple (Kafka only guarantees order within a partition) without exploding into 20+ topics for 6 real aggregates.

| Topic | Partition key | Consumer groups |
|---|---|---|
| order-events | orderId | payment-service, inventory-service, notification-service, delivery-service |
| payment-events | orderId | order-service, notification-service, settlement-service |
| inventory-events | productId or merchantId | notification-service |
| merchant-events | merchantId | catalog-service (optional), notification-service |
| delivery-events | orderId | order-service, notification-service |
| user-events | userId | user-service, payment-service (create wallet), notification-service |

- **DLQ:** every consumer group gets a matching `-dlt` topic. After N retries (Spring Kafka non-blocking retry topics, not blocking `Thread.sleep`), route to DLQ and alert.
- **Idempotency at consumer level:** each event carries a UUID `eventId`. Consumers either (a) check target-entity state before applying (preferred), or (b) fall back to a `processed_events(event_id)` table with a unique constraint.
- **Versioning:** add `schemaVersion` to event payloads from day one — costs nothing now, avoids a painful migration later.
- **Don't create a topic for** internal-only book-keeping nothing else reacts to (e.g. an audit log write).

## 10. Critical Workflows

**B — Merchant approval:** Admin approves → `merchant.approved` → notification sent → merchant-service transitions to `ONBOARDING` (waits for listings + hours before self-transitioning to `LIVE`). Reject → `REJECTED` (terminal), notification sent, no retry.

**C+D — Checkout + reservation:** order-service → REST → cart-service (get cart, single-merchant enforced) → REST → inventory-service (reserve per line item; 409 on insufficient stock releases already-reserved lines) → REST → payment-service (wallet or Razorpay order). Wallet success waits for `payment.success` on Kafka to reach `CONFIRMED`; wallet failure releases reservation.

**G — Merchant fulfillment:** `order.confirmed` → dashboard surfaces order → accept (`merchant.accepted` → `MERCHANT_ACCEPTED`) → preparing/ready (`order.ready` → `READY_FOR_PICKUP`) → reject (rare) → `merchant.rejected` → `MERCHANT_REJECTED` → refund workflow.

**H — Delivery assignment:** `order.ready` → delivery-service finds nearest available partner via Redis GEO query → assigns task → `delivery.assigned` → `DELIVERY_ASSIGNED`. Delivered → `delivery.delivered` → `DELIVERED` → `order.delivered` → settlement-service creates entry. No partner available → alert admin, stale-order signal in observability.

**I/J — Cancellation/Refund:** cancel only allowed pre-`DELIVERED` → `CANCELLED` → `order.cancelled` → inventory releases reservation, payment initiates refund → `payment.refunded` → `REFUNDED`. Cancelling an already-terminal order is a no-op, checked against current status first.

**K — Settlement:** `order.delivered` → `SettlementEntry(PENDING)` → daily batch job aggregates per merchant → `Payout` → simulated execution → `PAID`.

## 11. Idempotency — Where and How

| Area | Idempotency key | Mechanism |
|---|---|---|
| Order creation | client `Idempotency-Key` header, or "one active order per cart" | order-service rejects/returns-existing if key seen within short TTL (Redis) |
| Payment (wallet) | orderId | Unique constraint: one successful transaction per orderId |
| Payment webhook (Razorpay) | razorpay_payment_id | Unique constraint — duplicate delivery is a no-op |
| Inventory reservation | orderId + productId | Reservation row keyed by pair — re-reserving is a no-op, not a double-decrement |
| Kafka consumers (all) | eventId (UUID) | State-check-before-apply where possible; `processed_events` fallback |
| Refunds | orderId | Unique constraint on `refunds(order_id)` |
| Settlement | orderId | Unique constraint on `settlement_entries(order_id)` |

## 12. Resilience — Targeted, Not Blanket

| Pattern | Where | Protecting against |
|---|---|---|
| Circuit breaker | order-service → payment-service (Razorpay path) | Razorpay degradation shouldn't take down order creation for wallet-only customers too. |
| Retry (idempotent ops only) | order-service → inventory-service (reserve), order-service → payment-service (wallet pay) | Transient network blips — safe because both ops are idempotent (§11). |
| Timeout | Every Feign client | Prevents one slow downstream service from exhausting the caller's thread pool. |
| Bulkhead | order-service's Razorpay calls, isolated from internal-service calls | An external, higher-latency dependency shouldn't starve threads needed for fast internal calls. |
| Rate limiting | api-gateway, per-user | Abuse/scraping protection — Redis token bucket, same pattern as blinkit-clone. |

**Explicitly not wrapped:** internal reads to catalog-service (cacheable, low-stakes on a miss), notification-service calls (already async via Kafka — a resilience pattern on top of a resilience pattern isn't worth it).

## 13. Redis Architecture

| Use case | Key format | TTL | Owner | Invalidation |
|---|---|---|---|---|
| Cart | `cart:{userId}` | 7 days rolling | cart-service | On checkout or explicit clear |
| OTP | `otp:{phone/email}` | 5 min | auth-service | Expiry or successful verify |
| Refresh token | `refresh:{userId}` | 30 days | auth-service | Expiry, logout, or rotation on use |
| Access token blacklist | `blacklist:{token}` | remaining token TTL | auth-service (checked at gateway) | Natural expiry |
| Catalog read cache | `product:{productId}` | 10 min | catalog-service | TTL expiry; explicit bust on update |
| Delivery partner live location | Redis GEO `partnerlocations` | none (continuously overwritten) | delivery-service | Heartbeat-based removal (background job evicts after N min) |
| Gateway rate limiting | `ratelimit:{userId/IP}` | 1s window (token bucket) | api-gateway | Natural window rollover |

**Explicitly not in Redis:** inventory reservation locking (§4.4 — Postgres handles this), settlement/ledger data (needs durability Redis doesn't guarantee by default).

## 14. Security

- **JWT access token** (short-lived, ~15 min) + **opaque refresh token** (Redis-backed, rotatable on use).
- **RBAC as role + permission set**, not just a role string, so `SUPPORT_AGENT`/`MERCHANT_STAFF` can be added later as scoped permission bundles.
- **Service-to-service auth:** shared `X-Internal-Secret` header pattern (same as blinkit-clone) for V1. mTLS/OAuth2 client-credentials is the "more correct" production answer but meaningfully more operational complexity for a benefit that mostly matters once you have services you don't fully trust — documented as the next hardening step, not built now.
- **IDOR prevention:** every service trusts `X-User-Id` injected by the gateway from a validated JWT — never trust a `userId` in a request body. Ownership checks on every read/write of a customer-owned resource.
- **Password storage:** bcrypt.
- **Secrets:** environment variables for local/dev; a real secrets manager is a deployment-stage concern, not designed before there's a deployment target.
- **Duplicate-request protection:** covered by idempotency keys (§11), not a separate mechanism.

## 15. Observability

- **Correlation ID:** generated at api-gateway if absent, propagated via header through every Feign call and into Kafka event headers, put into MDC for structured logging.
- **Metrics (Micrometer → Prometheus):** per-service request latency (p50/p95/p99), error rate, Kafka consumer lag per topic/group, JVM/GC stats.
- **Business metrics worth a Grafana dashboard:**
  - Order funnel: created → payment success → merchant acceptance → delivered (spot drop-off)
  - Inventory oversell counter (should always be zero — a canary that §4.4 is actually working)
  - Kafka consumer lag per service
  - Payment success/failure rate by method (wallet vs Razorpay)
  - Stale `READY_FOR_PICKUP` orders (no delivery partner assigned within N minutes)

## 16. Deployment Architecture

See `DEPLOYMENT.md` for the full zero-cost deployment plan. Not needed until Stage 19–20 of the build order — nothing here blocks starting Stage 0/1.

## 17. Testing Strategy

- **Unit tests:** service-layer business logic with mocked repositories/clients — especially state-machine transitions (order status, merchant onboarding) and reservation math.
- **Integration tests (Testcontainers):** each service's actual database (Postgres/Mongo) + actual Redis where used.
- **Kafka tests (Testcontainers Kafka):** producer publishes correct event shape; consumer correctly applies idempotent state transition; DLQ receives a message after exhausted retries (deliberately trigger the failure).
- **End-to-end:** one critical-path test running the full checkout loop across real containerized services — order → reserve → pay → confirm → merchant accept → delivery assign → deliver → settlement entry created. The one test most worth having, because it proves the architecture, not just each service in isolation.

## 18. Documentation Structure

`docs/ARCHITECTURE.md` (this file), `docs/DEPLOYMENT.md`, `docs/BUILD_TRACKER.md`, `docs/ADR/` — one ADR per approved decision below, written at the time the decision is made, not retrofitted.

## 19. Decision Log (Approved)

| # | Decision | Recommended | Alternatives considered | Why | Consequence |
|---|---|---|---|---|---|
| 1 | Order scope | One merchant per order | Multi-merchant split orders | Splitting multiplies delivery/payment/settlement complexity for a feature most hyperlocal apps avoid | cart-service must enforce single-merchant rule |
| 2 | Inventory DB & concurrency | PostgreSQL, atomic conditional UPDATE | MongoDB + app-level locking; Redis distributed locks | Correctness comes from the DB natively; locks add a failure mode without added safety | inventory-service is the one Mongo-instinct exception — must be Postgres |
| 3 | Reservation timeout | DB-driven scheduled reaper job | Redis TTL/keyspace-notification eviction | Redis expiry events aren't guaranteed delivery; DB polling is boring but reliable | Small periodic job needed in inventory-service |
| 4 | Order status ownership | order-service holds canonical status, updated via Kafka events | Live API composition (query 3 services per page load) | Faster, simpler reads; small eventual-consistency lag acceptable | order-service must consume merchant/delivery Kafka events idempotently |
| 5 | Settlement service | Separate service, built last | Fold into payment-service | Different cadence/data shape/integration surface; clean boundary | One more service, but nothing else blocks on it |
| 6 | Roles beyond core 4 | No new roles/services now — permission scopes only | Build merchant-staff/support-agent now | No V1 payoff, real complexity cost | RBAC model must support permission bundles from day one |
| 7 | Catalog vs inventory split | Keep split | Merge into one service | Different DBs, read/write/consistency profiles | Two simpler, independently scalable services |
| 8 | Service-to-service auth | Shared internal-secret header | mTLS / OAuth2 client-credentials | Proven, low-complexity, adequate at this scale | Documented as a known future hardening item |
| 9 | Kafka topic granularity | One topic per aggregate + type field | One topic per event type (20+ topics) | Simpler ordering guarantees, fewer topics to operate | Every consumer must switch on event type |
| 10 | Merchant onboarding states | Cut UNDER_REVIEW as distinct state | Keep both | No behavioral difference, cosmetic only | One less state to test/handle everywhere |

Each decision has a matching ADR in `docs/ADR/`.
