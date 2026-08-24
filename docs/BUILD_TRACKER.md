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
- [x] `merchant-service`: apply, document metadata, store profile, hours, radius
- [x] Onboarding state machine: `PENDING_REVIEW → APPROVED/REJECTED → ONBOARDING → LIVE` — `SUSPENDED`/`TEMP_CLOSED`/`PERMANENTLY_CLOSED`
- [x] Admin approve/reject endpoints
- [x] Kafka: `merchant.approved`, `merchant.suspended`
- **Done when:** a merchant can apply, an admin can approve, illegal transitions are rejected, event fires on approval. ✅ Verified end-to-end through the gateway: apply → PENDING_REVIEW; duplicate apply → 409; go-live before approval → rejected (both the "hours not set" gate and, separately, the raw state-machine transition guard, confirmed independently); admin approve → APPROVED → ONBOARDING in one call, `merchant.approved` published to `merchant-events` with the correct key/payload, confirmed by actually consuming it off Redpanda; a non-admin (MERCHANT role, no `MERCHANT_APPROVE` permission) hitting an admin endpoint → 403; go-live after hours set → LIVE, appears in public `GET /api/merchants/nearby`; admin suspend → `merchant.suspended` published, no longer in `/nearby`; suspend-while-already-suspended → 409; reinstate → LIVE; reject → REJECTED, then reject-into-approve → 409 confirming REJECTED is terminal.

  One known gap surfaced during testing, not a Stage 3 blocker: there's no admin-account bootstrap flow yet — the ADMIN role had to be granted directly via SQL for testing. Worth a real fix (a seed admin, or an invite-only admin-creation endpoint) before any real deployment, but out of scope for the merchant-service work itself.

### Stage 4 — Catalog
- [x] `catalog-service` (MongoDB/Atlas): global product schema, categories
- [x] Search endpoint (by name/category)
- [x] Admin/merchant product-creation-request flow (moderation gate)
- [x] Kafka: `product.created`
- **Done when:** a product can be created and found via search; catalog is queryable without any merchant-specific data attached. ✅ Verified end-to-end through the gateway: 8 seeded categories and empty search both public with no token; unauthenticated product-request → 401; direct hit on catalog-service bypassing the gateway → 403 (`InternalRequestFilter`). Admin-created product → auto-`APPROVED`, immediately visible in search; unknown-category product → 400. Non-admin (no `CATALOG_MANAGE`) hitting an admin endpoint → 403. Merchant product-creation-request → `PENDING_REVIEW`, correctly absent from search and from the pending-not-yet-reviewed set; admin approve → `APPROVED`, now in search, `product.created` published to `product-events` and confirmed by actually consuming it off Redpanda; re-approving an already-`APPROVED` product → 409 (illegal transition). Separate reject flow: `PENDING_REVIEW → REJECTED` with a reason, stays out of search, and — confirmed by consuming the topic — does *not* publish an event (only approvals do). Admin category creation → 201, duplicate name → 409.

  MongoDB is self-hosted locally (`docker-compose.infra.yml`'s new `mongo` service) rather than Atlas — a deliberate, documented deviation from DEPLOYMENT.md's "Atlas even for local dev" default, since standing up a real Atlas cluster is explicitly a Stage 20 task, not a Stage 4 one; switching later is a one-line `MONGODB_URI` change with zero code changes.

  Two real issues caught and fixed during this stage, both before any functional testing: (1) api-gateway's public-paths list still had the Stage-1 placeholder `/api/catalog/**`, which would have exempted the new admin/merchant-protected catalog endpoints from JWT validation entirely — narrowed to the two actually-public browse endpoints. (2) `RoleSeeder` only ran its upsert once (`if (roleRepository.count() > 0) return`), so the new `CATALOG_MANAGE` permission would never have reached the `ADMIN` role on an already-seeded database — changed to an idempotent per-role/per-permission upsert that runs safely every boot. Also extracted the permission-header-parsing logic (previously duplicated between merchant-service and now catalog-service) into `common.security.PermissionUtil`, with each service keeping a thin wrapper for its own exception type.

  A handful of 503s showed up during manual testing, both right after a fresh `start-infra.sh` run and again after the dev machine woke from sleep mid-session — both times a plain retry succeeded. This is Spring Cloud LoadBalancer's client-side server cache lagging Eureka's registry (a missed-heartbeat/re-registration gap after a sleep, or the normal registration-propagation delay right after a service starts) rather than a routing or code bug — the same class of transient timing issue already seen and worked around in `start-infra.sh`'s Eureka-registry-snapshot retry loop.

### Stage 5 — Inventory (highest-risk stage — test concurrency explicitly)
- [x] `inventory-service` (Postgres): MerchantListing (price, stock per merchant-product)
- [x] Atomic reserve/confirm/release via conditional UPDATE
- [x] Reservation reaper job (scheduled, releases stale PAYMENT_PENDING reservations)
- [x] Concurrency test: N parallel reservation requests against stock=1, assert exactly one succeeds
- [x] Kafka: `inventory.low`, `inventory.out` produced; `product.created` consumed
- **Done when:** the concurrency test passes reliably (run more than once), reservation/release/confirm are each independently idempotent. ✅ Verified end-to-end: 20 parallel `reserve` requests (each a distinct orderId, quantity 1) fired concurrently against a listing with `availableStock=1` — exactly one got 201, the other 19 got 409, final state was `availableStock=0`/`reservedStock=1` with exactly one reservation row, no oversell and no phantom rows. Repeated against a second, independent listing with the same result — not a one-off. Idempotency confirmed directly: re-`reserve`-ing the same `(orderId, listingId)` pair is a no-op returning the existing row; `confirm`/`release` on an order with no `PAYMENT_PENDING` reservations left is a no-op (`[]`). Multi-item `reserve` where one line succeeds and a later line fails → whole call 409, and the JPA transaction rollback undoes the first line's decrement automatically (no manual compensating release needed for the DB side). Reservation reaper: created a reservation, backdated its `reserved_at` past the 15-minute timeout directly in Postgres, confirmed the scheduled job released it and restored `availableStock` on its next cycle. `inventory.out`/`inventory.low` confirmed on the `inventory-events` topic at the correct stock thresholds. `product.created` consumption confirmed: `KnownProduct` cache backfilled both products approved back in Stage 4, before this service ever existed (`auto-offset-reset: earliest` on a brand-new consumer group). Listing creation validates productId against that cache first, REST-falling-back to catalog-service on a miss — both paths exercised (a cached product landed instantly, an unknown productId correctly 400'd). Ownership/permission checks: non-merchant → 403 creating a listing; a merchant updating another merchant's listing → 403; direct hit on the reservation endpoints without the internal secret → 403; through the gateway without a JWT → 401 (these endpoints are internal-only — no user permission check, since the real caller is order-service, not a person, until Stage 8).

  Two real bugs found and fixed during this stage, both timing-related but for different reasons than earlier stages' load-balancer-cache lag: (1) `ProductEventConsumer`'s `earliest`-offset backfill runs a REST call to catalog-service immediately at container startup, before the `@LoadBalanced` RestTemplate's Eureka-derived view of `CATALOG-SERVICE` has populated — Spring Kafka's default near-instant retry backoff exhausted its attempts faster than that cache warmed up, silently skipping both historical `product.created` events (confirmed via `rpk group describe` showing the consumer had already advanced past them). Fixed with an explicit `DefaultErrorHandler` using a 3-second/8-attempt `FixedBackOff`, then replayed by resetting the consumer group's offset back to the start of the topic (`rpk group seek --to start`) — after which both products backfilled cleanly. (2) `KafkaTemplate.send` isn't covered by the surrounding `@Transactional` boundary: a multi-item `reserve()` call that decremented one listing's stock, then rolled back the *entire* transaction because a later line failed, still left a real `inventory.low` event on the topic for a stock change that had, at the database level, never actually happened — caught by literally counting topic offsets before/after the exact rollback scenario. Fixed by deferring the publish to `TransactionSynchronizationManager`'s `afterCommit` callback, so nothing is sent unless the transaction that produced the stock change actually commits; re-verified the same rollback scenario now produces zero events, while a real, committed threshold-crossing reserve still fires correctly.

### Stage 6 — Cart
- [x] `cart-service` (Redis): add/update/remove item, get cart
- [x] Single-merchant-per-cart rule enforced
- [x] Live price/availability check against catalog+inventory on cart read
- **Done when:** cart persists across requests, blocks a second merchant's item, reflects real-time stock/price. ✅ Verified end-to-end through the gateway: unauthenticated access → 401; empty cart for a fresh user; add an item → cart pins to that listing's merchant, response shows live price/stock/subtotal; adding a listing from a *different* merchant → 409 (ADR 0001), rejected without mutating the cart; adding an unknown/inactive listingId → 404. Cart persists across separate requests (confirmed via a plain GET after the add). Quantity update to more than currently available stock succeeds at write time but is correctly flagged `available:false` with `lineTotal:null` on read, and excluded from `subtotal` — updating a listingId not actually in the cart → 404. Removing the last item resets the cart's merchant pin to null, confirmed by then successfully adding an item from a *different* merchant. `DELETE /api/cart` clears it (Redis key actually gone, confirmed directly), and is idempotent — clearing an already-empty cart still returns 204. Redis TTL confirmed ~7 days on the key (`cart:{userId}`, ARCHITECTURE.md §12) and two different users' carts confirmed fully independent. Direct hit on cart-service's own port, bypassing the gateway → 403 (`InternalRequestFilter`).

  One small addition to inventory-service (Stage 5) was needed: cart-service's live availability check requires fetching a *specific* listing by its own id, which didn't exist yet (`ListingController` only had create/mine/update/by-productId). Added `GET /api/inventory/listings/{id}`, public in the sense that it needs no permission check, but *not* added to api-gateway's public-paths — it's only ever meant to be called service-to-service (cart-service's `InventoryClient`, mirroring inventory-service's own `CatalogClient` from Stage 5), confirmed by testing it both ways: 401 through the gateway without a JWT, 200 hit directly with just the internal secret.

  Kafka consumption of `order.confirmed` (to auto-clear a cart after checkout, per ARCHITECTURE.md §7's dependency table) is deliberately *not* built yet — it isn't in this stage's actual checklist above, and more importantly order-service (the only possible producer) doesn't exist until Stage 8, so there would be nothing real to consume or verify against. Revisit once Stage 8 ships. Coupon validation on cart read (§7's other listed dependency) is the same kind of gap for the same reason — coupon-service doesn't exist until Stage 7.

### Stage 7 — Coupons
- [x] `coupon-service` (Postgres): create/validate/apply, usage-limit enforcement
- [x] Atomic usage-count increment with limit check
- **Done when:** a coupon can't be used more than its configured limit under concurrent requests. ✅ Verified end-to-end through the gateway, including two independent concurrency scenarios: (1) a coupon with `usageLimit=1`, 20 parallel `apply` calls each with a *distinct* orderId → exactly one `201`, nineteen `409`s, final `usage_count=1` with exactly one usage row — no oversell. (2) A separate coupon with a high limit, 15 parallel `apply` calls all sharing the *same* orderId (a genuine double-submit/retry race) → all 15 return `201` with the identical result, and `usage_count` still only net-incremented by 1 with exactly one usage row — idempotency held under real concurrency, not just on sequential retries.

  `validate` (read-only) confirmed to never touch `usage_count`; discount math confirmed correct including the `maxDiscountAmount` cap (20% of 500 → capped from 100 down to 50) and the uncapped case (20% of 150 → 30); `minOrderValue` and unknown-code rejections both correct (400/404). Admin create/list/update all gated by `COUPON_MANAGE` (already seeded onto `ADMIN` since Stage 2's `RoleSeeder` — no RBAC changes needed this stage); duplicate coupon codes rejected (409); a deactivated coupon and one with a past `validUntil` are both correctly rejected on *both* `validate` and `apply`, not just one. Direct hit on coupon-service's own port, bypassing the gateway → 403; unauthenticated `apply` → 401.

  A real correctness bug was found and fixed *during design*, before it ever shipped: the initial `apply()` used a plain JPA insert wrapped in a try/catch for the same-orderId race (mirroring how a first instinct might handle a unique-constraint collision), compensating a spurious usage-count increment on failure. Under Postgres, though, any statement error — including a caught constraint violation — aborts the entire surrounding transaction until an explicit rollback; every subsequent statement in that same `@Transactional` method (the compensating decrement, the re-fetch of the winning row) would have then failed with "current transaction is aborted." Replaced with `INSERT ... ON CONFLICT (coupon_id, order_id) DO NOTHING` (`CouponUsageRepository.insertIfAbsent`), which never raises a SQL error in the first place — confirmed clean (zero errors in the service log) under the 15-way same-orderId concurrency test above, which specifically exercises this exact path.

  perUserLimit is checked transactionally but deliberately does *not* get the same hard atomic guarantee as the global `usageLimit` — the scenario it guards against (one user racing themselves across two simultaneous applies of the same coupon) is far lower-stakes than N different customers racing for one globally-limited code, which is what the required concurrency test above actually covers and what `tryIncrementUsage`'s atomic conditional UPDATE fully solves. This showed up naturally in test (1) above: of the 19 rejected requests, 2 were caught by the atomic global-limit check and 17 by the (by-then-accurate) per-user count — both are correct outcomes, and the atomic increment is what caught the ones the sequential per-user check couldn't have caught yet.

### Stage 8 — Order (orchestrator)
- [x] `order-service` (Postgres): place order — REST to cart, inventory, payment
- [x] Full status enum implemented, transitions gated (no illegal jumps)
- [x] Rollback path: reservation failure → release already-reserved items
- [x] Cancel order endpoint (pre-delivery states only)
- **Done when:** an order can be placed end-to-end with wallet payment, and a forced inventory-reservation failure correctly rolls back partial reservations. ✅ Verified end-to-end through the gateway.

  **Scope note, decided with the user before writing any code:** this stage's own "Done when" requires wallet payment to work, but wallet debit/credit is `payment-service`'s owned data (ARCHITECTURE.md §6), and payment-service is Stage 9 — a real sequencing gap in the roadmap itself, not something Stage 4-7 hit since each of those stages' own dependencies already existed. Resolved by pulling forward a minimal `payment-service` wallet slice now (`Wallet`, `WalletTransaction`, topup/balance/pay/refund, atomic debit via ADR 0002's pattern, `user.registered`-driven auto-provisioning, `payment.success`/`payment.failed` events) — enough for a real, non-stubbed end-to-end test. Razorpay, HMAC webhook verification, and the fuller ledger remain genuine Stage 9 scope.

  **Happy path**, verified through actual checkout, not just individual endpoint checks: cart → order → `CREATED` → coupon (if any) → inventory `reserve` → `PAYMENT_PENDING` → wallet `pay` → inventory `confirm` (permanently consumes the reservation, not left dangling in `PAYMENT_PENDING` for Stage 5's reaper to incorrectly undo) → `CONFIRMED`. Wallet correctly debited the *coupon-discounted* total, not the subtotal. `order.created`/`order.confirmed`/`payment.success` all confirmed on their real topics.

  **Both required rollback scenarios verified concretely, not just unit-level:**
  - *Reservation itself fails:* two customers, one unit of stock, both carts independently showing `available:true` (neither has reserved yet) — fired both `POST /api/orders` concurrently. One `CONFIRMED`, the other genuinely hit inventory's 409 and came back `CANCELLED` with reason "Insufficient stock" — no oversell, the loser's wallet was never touched. (A naive "add 999 to cart" test doesn't actually exercise this path — cart-service's own live-availability check from Stage 6 catches that earlier and rejects the whole checkout before order-service ever calls reserve; the real race above is what actually reaches inventory's atomic UPDATE.)
  - *Reservation succeeds, payment fails:* a customer with a $0 wallet reserved successfully, then hit 402 on `pay` — order-service called inventory's `release` and the order landed on `PAYMENT_FAILED`, with the listing's `availableStock` confirmed back to its pre-reservation value.

  **Cancel-and-refund**, tested against a real coupon-discounted `CONFIRMED` order: cancel → inventory `release` (synchronous, since inventory-service doesn't consume `order-events` per §7's table) + `order.cancelled` published (asynchronous — payment-service's consumer issues the refund; §8's REST-vs-Kafka rule says order-service doesn't need the refund's result to decide anything next) → wallet credited the exact discounted amount that was actually charged. Re-cancelling the same order, and cancelling a `PAYMENT_FAILED` order, both correctly rejected as illegal transitions (409) — the guard is the same `OrderStatusTransition` EnumMap pattern as Stage 3's `MerchantStatusTransition`, built for the *full* lifecycle (through `DELIVERED`/`REFUNDED`) even though this stage only ever drives `CREATED` through `CONFIRMED`/`PAYMENT_FAILED`/`CANCELLED` itself — `MERCHANT_ACCEPTED` onward has no real producer yet (no order-acceptance endpoint on merchant-service, no delivery-service until Stage 11), expected and consistent with how every previous stage built ahead of its own first consumer.

  Also verified: ownership (403 viewing another user's order), `GET /mine`/`GET /{id}`/404-on-unknown, direct-hit bypass on both new services (403), unauthenticated place-order (401), and coupon per-user-limit correctly carrying through the *whole* order flow (a second attempt at an already-used coupon cancels the order with a clear reason, not a raw 409 the customer never sees).

  **A real, user-visible gap found and closed mid-stage:** the happy-path test showed the customer's cart still full of items they'd just paid for. ARCHITECTURE.md §7 already specifies the fix — cart-service consuming `order.confirmed` to clear itself — and Stage 6 had explicitly deferred exactly this, since order-service (the only possible producer) didn't exist yet. It does now, so this stage closes that gap: `order.created`/`order.confirmed`'s payload was widened to carry `userId` (not just `orderId`, since an async consumer needs enough in the event to act, not a callback to ask who the order belonged to), and cart-service gained its first Kafka dependency — deliberately `auto-offset-reset: latest`, not `earliest` like every read-model-backfill consumer so far, since there's no correct action to take on a stale, pre-existing `order.confirmed`: a cart's contents today aren't necessarily what was in it when some old order was placed, and "catching up" could clear a cart full of unrelated newer items.

  **Two design points worth being explicit about:**
  - `placeOrder` has no `@Transactional` anywhere, and deliberately can't have one wrapping the whole flow — that would repeat the exact class of bug fixed in Stage 5 (a Kafka publish inside a transaction that later rolled back), generalized to "a REST call to another service inside a transaction that might still roll back." Each status transition is exactly one `orderRepository.save(...)` call, which Spring Data's `JpaRepository` already commits transactionally on its own — no explicit `@Transactional` needed, and importantly, adding one to these small helper methods wouldn't even have worked: they're called via same-class (`this.foo(...)`) invocation from `placeOrder`, which bypasses Spring's transactional proxy entirely, a well-known pitfall this design sidesteps by not needing the proxy in the first place.
  - Every cross-service call order-service makes propagates the *caller's own already-gateway-verified* `X-User-Id` manually (cart, coupon) alongside the shared `X-Internal-Secret` — the same trust model used everywhere else in the project (a request that already passed through the gateway once can have that verified identity forwarded by a trusted internal caller), not a new pattern.

### Stage 9 — Payment
- [x] `payment-service` (Postgres): wallet debit/credit — pulled forward into Stage 8 (see its notes for why); transaction ledger still minimal (`TOPUP`/`DEBIT`/`CREDIT` rows, no richer reporting yet)
- [ ] Razorpay order creation + HMAC-SHA256 webhook signature verification
- [ ] Idempotency: unique constraint on `razorpay_payment_id`, on `orderId` for wallet — the `orderId`-for-wallet half is already done and verified (Stage 8's `apply`/`refund` race tests), `razorpay_payment_id` isn't yet, no Razorpay integration exists yet
- [x] Kafka: `payment.success`, `payment.failed` — done in Stage 8, publishing correctly on both the wallet `pay` and (implicitly) the refund paths
- **Done when:** both wallet and Razorpay payment paths confirm an order; replaying the same webhook twice does not double-process. Wallet half already verified in Stage 8. Remaining Stage 9 scope is genuinely just Razorpay: order creation, the HMAC webhook handler, and its own idempotency on `razorpay_payment_id`.

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
| 16 | Secrets (Postgres credentials, internal-request secret, JWT signing key) sourced from a gitignored `.env`, never committed as YAML literals |

## 4. How to Use This File

- Check boxes as you go, per stage. Don't skip a stage's "Done when" criterion — that line is the actual acceptance test, not a suggestion.
- If a decision needs to change mid-build, update the Decision Log here **and** the corresponding ADR in `docs/ADR/` — don't let this file and the ADRs drift apart.
- Stage 5 (inventory concurrency) and Stage 10 (event wiring + DLQ) are the two stages most worth not rushing — they're where the architecture's actual claims (no oversell, no lost/duplicated events) get proven or disproven.
