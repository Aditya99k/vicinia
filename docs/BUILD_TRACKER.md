# Vicinia — Master Build Tracker

Single working checklist for the build. Reasoning lives in `ARCHITECTURE.md` (product/services/data/events) and `DEPLOYMENT.md` (infra/hosting) — this file is deliberately just the checklist, meant to be checked off as you go. Don't relitigate decisions here; update the source doc + matching ADR instead.

Status legend: `[ ]` not started · `[~]` in progress · `[x]` done

## 0. Quick Reference

**Product:** Hyperlocal multi-merchant commerce platform — merchants self-onboard, customers order from one nearby store at a time, delivery partners fulfill, platform takes commission + handles settlement.

**Roles (final, 4):** CUSTOMER · MERCHANT (owner) · DELIVERY_PARTNER · ADMIN (RBAC built as role+permission, so SUPPORT_AGENT/MERCHANT_STAFF can be added later without a redesign — not built in V1.)

**Services (16 + 3 infra = 19 total):**
- Infra: config-server · discovery-server · api-gateway
- Domain: auth · user · merchant · catalog · inventory · cart · coupon · order · payment · delivery · notification · review · settlement

**Database ownership (final):**

| DB | Services | Hosting |
|---|---|---|
| PostgreSQL | auth, user, merchant, inventory, coupon, order, payment, delivery, settlement | Self-hosted on Oracle VM, 1 instance / 9 logical DBs |
| MongoDB | catalog, notification, review | Atlas M0 (managed, free) |
| Redis | cart, OTP, refresh tokens, blacklist, rate-limit, delivery GEO | Self-hosted on Oracle VM |
| Kafka-protocol | all async events | Redpanda, self-hosted on Oracle VM |

**Deployment target:** Oracle Cloud Always Free ARM VM (2 OCPU/12GB) running Docker Compose · MongoDB Atlas M0 · Cloudflare Pages (frontend/CDN) · Cloudflare Tunnel (gateway exposure/TLS) · Cloudinary (images) · GitHub Actions (CI/CD) · GitHub Container Registry. Full reasoning + RAM budget in `DEPLOYMENT.md`.

## 1. Full Feature Checklist (by actor — scope reference, not stage order)

**Customer**
- [ ] Register / login (JWT + refresh)
- [ ] Logout (token blacklist)
- [ ] Profile + saved addresses
- [ ] Browse merchants near current location
- [ ] Browse categories
- [ ] Search products (cross-merchant, ranked by distance/price)
- [ ] Product detail page (single merchant listing)
- [ ] Cart (single-merchant enforced)
- [ ] Apply coupon
- [ ] Checkout (address + payment method select)
- [ ] Pay — wallet
- [ ] Pay — Razorpay
- [ ] Place order
- [ ] Track order status (live)
- [ ] Cancel order (pre-delivery states only)
- [ ] Order history
- [ ] Refund visibility
- [ ] Rate/review a product (post-delivery only)

**Merchant**
- [ ] Merchant registration + document submission
- [ ] View onboarding status
- [ ] Store profile (name, hours, delivery radius, location)
- [ ] Add/edit catalog listings (price, stock, per product)
- [ ] View incoming orders
- [ ] Accept / reject order
- [ ] Mark preparing → ready for pickup
- [ ] View settlement/payout history

**Delivery Partner**
- [ ] Registration + verification
- [ ] Online/offline toggle
- [ ] Live location updates
- [ ] View assigned task
- [ ] Accept / reject assignment
- [ ] Update status: picked up → out for delivery → delivered
- [ ] Delivery history

**Admin**
- [ ] Merchant approval / rejection queue
- [ ] Merchant suspend / reinstate
- [ ] Customer account management (view/disable)
- [ ] Delivery partner management
- [ ] Global category management
- [ ] Coupon management (create/edit/deactivate)
- [ ] Order visibility (all orders, filter/search)
- [ ] Payment/refund visibility
- [ ] Manual refund trigger
- [ ] Platform metrics dashboard (Grafana, not custom-built)

## 2. Stage-by-Stage Build Checklist

### Stage 0 — Foundations
- [x] Repo created, monorepo structure (`backend/`, `frontend/`, `docs/`)
- [x] `docs/ARCHITECTURE.md`, `docs/DEPLOYMENT.md`, this tracker committed
- [ ] `docs/ADR/` folder started, one ADR per decision in `ARCHITECTURE.md` §19
- [ ] Root parent `pom.xml` (multi-module Maven, Spring Cloud BOM)
- **Done when:** repo exists, docs committed, `mvn validate` runs clean on an empty parent POM.

### Stage 1 — Infra Skeleton
- [x] `config-server` — serves shared `application.yml` + per-service overrides
- [x] `discovery-server` (Eureka)
- [x] `api-gateway` — routes table for all 13 domain services, public-path list
- [x] `docker-compose.infra.yml` — Redis, Redpanda (single-node), Postgres
- **Done when:** gateway + eureka + config all show UP on `/actuator/health`, gateway dashboard shows itself registered. ✅ Verified locally: all three services report `UP`, api-gateway registered in Eureka as `API-GATEWAY`, config-server serves the shared config to it, and Postgres/Redis/Redpanda are all healthy with the 9 logical databases created.

### Stage 2 — Auth & User
- [x] `auth-service`: signup, login, refresh, logout, forgot/reset password
- [x] JWT issuing (access short-lived, refresh in Redis), token blacklist on logout
- [x] Internal-request-filter pattern applied (gateway-only access)
- [x] `user-service`: profile, address CRUD
- [x] Kafka: `user.registered`, `user.deleted` wired
- [x] RBAC: role + permission model (not just role string) in place
- **Done when:** can sign up, log in, hit a protected user-service endpoint with the access token, refresh after expiry, and log out (token then rejected). ✅ Verified locally end-to-end through api-gateway: signup → RBAC roles/permissions on the token → user-service profile auto-created via the `user.registered` Kafka event → refresh (old refresh token rejected after rotation) → new access token works → logout → same token now rejected (401). Also verified: duplicate-email signup (409), self-registering as ADMIN (400), wrong password (401), forgot/reset-password with one-time token, and that hitting auth-service/user-service directly on their own ports (bypassing the gateway) is rejected (403) by InternalRequestFilter.

  Two real bugs found and fixed during verification: (1) `docker-compose.infra.yml` mapped Redpanda's pandaproxy/schema-registry to host ports 8081/8082, colliding with auth-service/user-service — remapped to 18081/18082. (2) Redpanda advertised `redpanda:9092` (only resolvable inside the Docker network), but services run on the host at this stage — changed to advertise `localhost:9092` with a comment noting this flips back to the container hostname once app services are containerized in Stage 19. Also caught a Spring bean-name collision: a custom `GatewayProperties` class collided with Spring Cloud Gateway's own internal bean of the same name — renamed to `PublicPathsProperties`.

### Stage 3 — Merchant
- [ ] `merchant-service`: apply, document metadata, store profile, hours, radius
- [ ] Onboarding state machine: `PENDING_REVIEW → APPROVED/REJECTED → ONBOARDING → LIVE` — `SUSPENDED`/`TEMP_CLOSED`/`PERMANENTLY_CLOSED`
- [ ] Admin approve/reject endpoints
- [ ] Kafka: `merchant.approved`, `merchant.suspended`
- **Done when:** a merchant can apply, an admin can approve, illegal transitions are rejected, event fires on approval.

### Stage 4 — Catalog
- [ ] `catalog-service` (MongoDB/Atlas): global product schema, categories
- [ ] Search endpoint (by name/category)
- [ ] Admin/merchant product-creation-request flow (moderation gate)
- [ ] Kafka: `product.created`
- **Done when:** a product can be created and found via search; catalog is queryable without any merchant-specific data attached.

### Stage 5 — Inventory (highest-risk stage — test concurrency explicitly)
- [ ] `inventory-service` (Postgres): MerchantListing (price, stock per merchant-product)
- [ ] Atomic reserve/confirm/release via conditional UPDATE
- [ ] Reservation reaper job (scheduled, releases stale PAYMENT_PENDING reservations)
- [ ] Concurrency test: N parallel reservation requests against stock=1, assert exactly one succeeds
- [ ] Kafka: `inventory.low`, `inventory.out` produced; `product.created` consumed
- **Done when:** the concurrency test passes reliably (run more than once), reservation/release/confirm are each independently idempotent.

### Stage 6 — Cart
- [ ] `cart-service` (Redis): add/update/remove item, get cart
- [ ] Single-merchant-per-cart rule enforced
- [ ] Live price/availability check against catalog+inventory on cart read
- **Done when:** cart persists across requests, blocks a second merchant's item, reflects real-time stock/price.

### Stage 7 — Coupons
- [ ] `coupon-service` (Postgres): create/validate/apply, usage-limit enforcement
- [ ] Atomic usage-count increment with limit check
- **Done when:** a coupon can't be used more than its configured limit under concurrent requests.

### Stage 8 — Order (orchestrator)
- [ ] `order-service` (Postgres): place order — REST to cart, inventory, payment
- [ ] Full status enum implemented, transitions gated (no illegal jumps)
- [ ] Rollback path: reservation failure → release already-reserved items
- [ ] Cancel order endpoint (pre-delivery states only)
- **Done when:** an order can be placed end-to-end with wallet payment, and a forced inventory-reservation failure correctly rolls back partial reservations.

### Stage 9 — Payment
- [ ] `payment-service` (Postgres): wallet debit/credit, transaction ledger
- [ ] Razorpay order creation + HMAC-SHA256 webhook signature verification
- [ ] Idempotency: unique constraint on `razorpay_payment_id`, on `orderId` for wallet
- [ ] Kafka: `payment.success`, `payment.failed`
- **Done when:** both wallet and Razorpay payment paths confirm an order; replaying the same webhook twice does not double-process.

### Stage 10 — Event-Driven Wiring Pass
- [ ] Redpanda topics created: order-events, payment-events, inventory-events, merchant-events, delivery-events, user-events
- [ ] Idempotent consumers on every listener
- [ ] Retry topics + DLQ per consumer group
- [ ] Deliberately trigger one failure and confirm it lands in the DLQ
- **Done when:** order-service's status correctly reflects `payment.success`/`payment.failed` end-to-end, and the DLQ test passes.

### Stage 11 — Delivery
- [ ] `delivery-service` (Postgres + Redis GEO): partner registration, online/offline
- [ ] Live location via `GEOADD`, nearest-partner query via `GEORADIUS`
- [ ] Assignment on `order.ready`, accept/reject, status updates
- [ ] Kafka: `delivery.assigned`, `delivery.delivered` consumed by order-service
- **Done when:** an order in `READY_FOR_PICKUP` gets assigned to the nearest online partner, and status updates propagate back into `order.status`.

### Stage 12 — Notification
- [ ] `notification-service` (MongoDB): consume `user.registered`, `order.confirmed`, `payment.failed`, `inventory.low`
- [ ] Email send (or logged-only stub if no real provider wired)
- **Done when:** each consumed event produces a logged/sent notification record.

### Stage 13 — Review
- [ ] `review-service` (MongoDB): create review, gated by "has this user ordered this product" via Feign to order-service
- [ ] Product rating aggregation endpoint
- **Done when:** a review can only be created after a delivered order containing that product; rating aggregate updates correctly.

### Stage 14 — Settlement
- [ ] `settlement-service` (Postgres): SettlementEntry created on `order.delivered`
- [ ] Scheduled batch job aggregates entries → Payout
- [ ] Simulated payout execution (PENDING → PROCESSING → PAID)
- **Done when:** a delivered order produces a settlement entry with correct commission math, batch job correctly groups entries per merchant.

### Stage 15 — Resilience Hardening
- [ ] Circuit breaker: order-service → payment-service (Razorpay path)
- [ ] Retry (idempotent-only): order-service → inventory-service, order-service → payment-service
- [ ] Timeout on every Feign client
- [ ] Bulkhead: Razorpay calls isolated from internal-service calls
- [ ] Gateway rate limiting (Redis token bucket)
- **Done when:** killing the Razorpay-dependent path degrades gracefully without exhausting threads needed for wallet-only checkout.

### Stage 16 — Observability
- [ ] Correlation ID: generated at gateway, propagated via header + into Kafka headers, MDC logging
- [ ] Micrometer → Prometheus on every service
- [ ] Grafana dashboards: order funnel, inventory-oversell counter (should be zero), Kafka consumer lag, payment success/failure rate, stale READY_FOR_PICKUP orders
- **Done when:** a single order's correlation ID can be traced across logs in ≥4 different services, and all 5 dashboards render real data.

### Stage 17 — Testing Hardening Pass
- [ ] Unit tests: state-machine transitions (order, merchant onboarding), reservation math
- [ ] Integration tests (Testcontainers): each service against its real DB
- [ ] Kafka tests (Testcontainers): producer shape, idempotent consumer, DLQ path
- [ ] One full e2e test: checkout → confirm → merchant accept → delivery → deliver → settlement
- **Done when:** the e2e test passes in CI, not just locally.

### Stage 18 — Frontend
- [ ] Customer app: browse, cart, checkout, order tracking, profile
- [ ] Merchant dashboard: onboarding status, listings, order queue
- [ ] Delivery agent app: assignment, status updates, live location ping
- [ ] Admin dashboard: approvals, order/payment visibility, coupon management
- **Done when:** all 4 role-specific flows work against the real backend (not mocked).

### Stage 19 — Packaging & CI/CD
- [ ] `docker-compose.prod.yml` covering all 19 services + Redpanda + Postgres + Redis
- [ ] GitHub Actions: build → test → image push to GHCR on merge to main
- [ ] Deploy step: SSH to Oracle VM, `docker compose pull && up -d`
- **Done when:** a merge to main results in the running VM updating automatically.

### Stage 20 — Deployment (Live)
- [ ] Oracle Cloud Always Free VM provisioned (do this early — capacity/approval risk, see `DEPLOYMENT.md` §6)
- [ ] MongoDB Atlas M0 project created
- [ ] Cloudinary account + upload preset configured
- [ ] Cloudflare Pages project (frontend) + Cloudflare Tunnel (gateway) configured
- [ ] Domain/hostname decided: owned domain, or sslip.io wildcard fallback
- [ ] Full stack smoke-tested against the live URLs, not just localhost
- **Done when:** a stranger with the public URL can sign up, browse, and place a real order against the live deployment.

## 3. Decision Log (finalized — full reasoning in `ARCHITECTURE.md` §19)

| # | Decision |
|---|---|
| 1 | Single merchant per order — no split orders in V1 |
| 2 | Inventory on PostgreSQL, atomic conditional updates, no Redis locks |
| 3 | Reservation timeout via DB-driven scheduled reaper, not Redis TTL events |
| 4 | order-service owns canonical status, fed by Kafka events from merchant/delivery |
| 5 | Settlement is a separate service, built last (Stage 14) |
| 6 | No merchant-staff/support-agent roles — RBAC permission scopes only, deferred |
| 7 | Catalog and inventory kept as separate services |
| 8 | Service-to-service auth: shared internal-secret header (not mTLS) |
| 9 | Kafka: one topic per aggregate + type field, not one topic per event |
| 10 | Merchant onboarding: UNDER_REVIEW cut as a distinct state |
| 11 | Compute: single Oracle Always Free VM, Docker Compose, no k3s until justified |
| 12 | Kafka replaced with Redpanda (self-hosted, protocol-compatible) for RAM reasons |
| 13 | Postgres self-hosted (9 logical DBs) instead of managed free tier (autosuspend risk) |
| 14 | MongoDB stays on Atlas M0 (managed, no reason to self-host) |
| 15 | Frontend: Cloudflare Pages; Gateway exposure: Cloudflare Tunnel |

## 4. How to Use This File

- Check boxes as you go, per stage. Don't skip a stage's "Done when" criterion — that line is the actual acceptance test, not a suggestion.
- If a decision needs to change mid-build, update the Decision Log here **and** the corresponding ADR in `docs/ADR/` — don't let this file and the ADRs drift apart.
- Stage 5 (inventory concurrency) and Stage 10 (event wiring + DLQ) are the two stages most worth not rushing — they're where the architecture's actual claims (no oversell, no lost/duplicated events) get proven or disproven.
