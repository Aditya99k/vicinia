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
- [x] Razorpay order creation + HMAC-SHA256 webhook signature verification
- [x] Idempotency: unique constraint on `razorpay_payment_id`, on `orderId` for wallet
- [x] Kafka: `payment.success`, `payment.failed` — done in Stage 8, publishing correctly on both the wallet `pay` and (implicitly) the refund paths
- **Done when:** both wallet and Razorpay payment paths confirm an order; replaying the same webhook twice does not double-process. ✅ Verified end-to-end through the gateway, including a real live call to Razorpay's test-mode API (the user provided real `rzp_test_` credentials — test mode, cannot move real money) — order creation returned a genuine Razorpay order id, not a stub.

  **The async path, proven both directions:** placing an order with `paymentMethod: RAZORPAY` reserves inventory exactly like wallet, but stays `PAYMENT_PENDING` and returns `razorpayOrderId`/`razorpayKeyId` — everything a real frontend needs to open Checkout.js, once Stage 18 builds one. Resolution happens later, entirely through the webhook: a correctly-HMAC-signed `payment.captured` payload (constructed locally — see below) → order-service's new Kafka consumer → `CONFIRMED`, inventory `confirm`'d (permanently consumed, not left dangling). A `payment.failed` payload on a second order → `PAYMENT_FAILED`, inventory `release`'d, reservation confirmed restored — the same two outcomes Stage 8 proved for wallet, now proven for the genuinely-async path too.

  **Idempotency, proven concretely, not just by inspection:** replaying the *exact same* webhook delivery a second time → still `200` (Razorpay expects an ack, not a retry-provoking error), topic high-watermark unchanged (no duplicate `payment.success`), no errors logged. The atomic conditional UPDATE (`tryResolve`, ADR 0002's pattern for a fifth time now — inventory, coupons, wallet pay, wallet refund, and this) is the actual mechanism; the DB-level unique constraints on both `order_id` and `razorpay_payment_id` (confirmed present via `\d razorpay_payments`) are a defense-in-depth backstop, not the primary guard. Signature verification tested both ways: a tampered signature → `400`, rejected before any DB or Kafka side effect; a webhook for an unrecognized `razorpay_order_id` → `404`.

  **No real webhook could be registered locally** (no public HTTPS endpoint), so `RAZORPAY_WEBHOOK_SECRET` is a locally-generated stand-in rather than a Razorpay-dashboard value — documented in `.env`/`.env.example`. This didn't limit what could actually be tested: the HMAC verification logic is exactly what ships to production regardless of where the secret comes from, and constructing correctly-signed test payloads by hand (same `HMAC-SHA256(rawBody, secret)` Razorpay's own servers compute) exercises the real code path, including the "reject a tampered payload" direction a passive/mocked test couldn't prove.

  **Wallet's synchronous behavior confirmed unchanged** (a real regression check, not an assumption) — placing a wallet order after all the Razorpay work still returns `CONFIRMED` synchronously in the same response, `razorpayOrderId`/`razorpayKeyId` both `null`. `payment.success`/`payment.failed` payloads now carry a `method` field (`"WALLET"` vs `"RAZORPAY"`) specifically so order-service's new consumer can ignore wallet-originated events it already handled synchronously — confirmed both that a `WALLET`-tagged event through the new consumer produced no errors and that the wallet checkout's own event was correctly tagged, not just that the consumer's filter *looks* correct in code.

### Stage 10 — Event-Driven Wiring Pass
- [x] Redpanda topics created: order-events, payment-events, inventory-events, merchant-events, delivery-events, user-events
- [x] Idempotent consumers on every listener
- [x] Retry topics + DLQ per consumer group
- [x] Deliberately trigger one failure and confirm it lands in the DLQ
- **Done when:** order-service's status correctly reflects `payment.success`/`payment.failed` end-to-end, and the DLQ test passes. ✅ Verified end-to-end. `payment.success`/`payment.failed` → `CONFIRMED`/`PAYMENT_FAILED` was already proven concretely in Stage 9's real Razorpay webhook tests — this stage's own job was making every consumer's failure path as reliable as its happy path.

  **Topics:** all 6 aggregate topics (`order-events`, `payment-events`, `inventory-events`, `merchant-events`, `delivery-events`, `user-events`) already exist, auto-created through genuine producer usage across Stages 2–9 — confirmed directly via `rpk topic list` rather than re-provisioning something already real. `delivery-events`/`merchant-events` have producers but no consumers yet (delivery-service doesn't exist until Stage 11) — expected, consistent with every event shipped ahead of its first consumer throughout this project.

  **Idempotency audit** of all 5 existing consumers (`UserEventConsumer`, `ProductEventConsumer`, `OrderConfirmedConsumer`, `PaymentEventConsumer`, `PlatformEventConsumer`) confirmed each was already idempotent by construction — a state-check before mutating (does the profile/wallet already exist, is the order still `PAYMENT_PENDING`, is deleting an already-gone Redis key a no-op) — ARCHITECTURE.md §11's preferred approach over a `processed_events` table. No consumer needed new idempotency logic; this stage's real work was the retry/DLQ layer around them.

  **`@RetryableTopic` added to all 5 listeners** (ARCHITECTURE.md §9 — "Spring Kafka non-blocking retry topics, not blocking Thread.sleep"), migrating inventory-service's `ProductEventConsumer` off the custom `DefaultErrorHandler` bean it had used since Stage 5 for the same underlying reason (a cold-start REST call racing the load-balancer cache) — same effective backoff window (~15–18s), now via the architecturally-intended mechanism instead of a one-off fix. Each listener also got a `@DltHandler` that logs at ERROR level; real alerting on that is Stage 16's job, not this one's.

  **Two real bugs found by actually triggering the DLQ test, not by inspection:**
  - `cart-service` and `user-service` had never published anything of their own before this stage — `@RetryableTopic`'s internal republishing to the retry/DLT topics needs a real producer, and without an explicit `spring.kafka.producer` block, Spring Boot's autoconfigured `KafkaTemplate` defaults to a plain `StringSerializer`. Trying to republish a deserialized envelope *object* through it threw `ClassCastException`, discovered only once a deliberate failure actually tried to exercise that path. Fixed by adding the same explicit producer config every other service already had.
  - Spring Kafka's default retry/DLT topic naming is per **source topic**, not per **consumer group** — so `cart-service` and `payment-service` (both consuming `order-events`), and separately `user-service` and `payment-service` (both consuming `user-events`), collided onto the exact same default-named DLT. A failure in one service's consumer was landing on a topic the *other* service's `@DltHandler` was also subscribed to, triggering its unrelated log line. Confirmed directly: publishing one malformed `order.confirmed` event caused *both* `cart-service`'s and `payment-service`'s DLQ handlers to fire. Fixed by giving every listener an explicit, service-qualified `retryTopicSuffix`/`dltTopicSuffix` — applied to all 5 consumers for consistency, not just the 2 colliding today, since the same collision would silently recur the moment any topic gains a second consumer (e.g. `payment-events` once notification-service, Stage 12, joins it).

  **The deliberate-failure test itself:** published a hand-crafted `order.confirmed` event directly to `order-events` with a payload `userId` that isn't a valid UUID (`UUID.fromString` throws deterministically, no external dependency needed to make it fail reliably) — retried 3 times on `order-events-cart-service-retry` (confirmed via real retry-topic message counts, not just a status code), then landed on `order-events-cart-service-dlt` with `cart-service`'s `@DltHandler` logging it, while `order-events-payment-service-dlt` stayed empty. Re-ran twice: once before the naming fix (both DLQ handlers fired, confirming the bug) and once after (only the correct one fired, confirming the fix) — and a full happy-path checkout (wallet topup → order → `CONFIRMED` → cart cleared) re-verified clean afterward, confirming none of this broke normal operation.

### Stage 11 — Delivery
- [x] `delivery-service` (Postgres + Redis GEO): partner registration, online/offline
- [x] Live location via `GEOADD`, nearest-partner query via `GEORADIUS`
- [x] Assignment on `order.ready`, accept/reject, status updates
- [x] Kafka: `delivery.assigned`, `delivery.delivered` consumed by order-service
- **Done when:** an order in `READY_FOR_PICKUP` gets assigned to the nearest online partner, and status updates propagate back into `order.status`. ✅ Verified end-to-end against the real running stack.

  **Sequencing gap resolved first:** Stage 11 needs a real `order.ready` event to assign against, but merchant-service had no accept/prepare/mark-ready capability — that was never its own stage, it just hadn't been built. Pulled forward a minimal order-management slice into merchant-service (mirroring the Stage 8 payment-service precedent of building ahead of a stage's own first consumer): `MerchantOrderTask` (own local state, `PENDING_ACCEPTANCE → ACCEPTED/REJECTED`, `ACCEPTED → READY`), auto-created from `order.confirmed` via merchant-service's first-ever Kafka consumer, and `POST /api/merchants/orders/{orderId}/{accept,reject,ready}` publishing `merchant.accepted`/`merchant.rejected`/`order.ready` back onto **`order-events`** (not `merchant-events` — these describe the order aggregate, per ADR 0009, not the producing service). `order.ready`'s payload carries the store's own lat/lng directly (validated non-null first, else `StoreLocationNotSetException`), so delivery-service never needs a callback to ask where to search.

  **Full happy path**, built from scratch against a live stack (fresh merchant/customer/delivery-partner accounts, admin promoted via direct SQL per the Stage 3-documented gap — still no admin bootstrap flow): delivery-partner auto-provisioned from `user.registered` (confirmed `GET /api/delivery/partners/me` → `OFFLINE` immediately after signup, no manual action) → partner goes online with a location (`POST /partners/online` → confirmed via `GEOPOS` that Redis keys the GEO set by `DeliveryPartner.id`, not `userId`, matching `DeliveryTask.partnerId`'s type) → customer places a real order (wallet top-up → cart → checkout) → merchant accepts (`CONFIRMED → MERCHANT_ACCEPTED → PREPARING` double-hop, order-service) → merchant marks ready (`order.ready` fires with real lat/lng) → delivery-service's `OrderReadyConsumer` creates a `DeliveryTask` and `attemptAssignment` finds the online partner via `GEORADIUS`, publishes `delivery.assigned` → order-service's `DeliveryEventConsumer` moves the order to `DELIVERY_ASSIGNED` → partner accepts → picked-up → delivered (`DELIVERY_ASSIGNED → OUT_FOR_DELIVERY → DELIVERED` double-hop on `delivery.delivered`, order-service).

  **Reassignment-on-reject:** a second order, assigned to the nearer of two online partners; that partner rejects → task cycles back to `PENDING_ASSIGNMENT` with the rejecting partner added to `excludedPartnerIds`, `attemptAssignment` re-runs immediately and reassigns to the second partner (confirmed via `delivery_task_excluded_partners`) — deliberately not a terminal `REJECTED` status, since the order still needs delivery; rejection is a fact about one partner's declined offer, not the order.

  **`AssignmentReaper`** confirmed working, not just present: took both partners offline, marked a third order ready (task correctly sat at `PENDING_ASSIGNMENT` with no partner, since `attemptAssignment`'s first pass found nobody), brought one partner back online, and watched the scheduled reaper (30s interval) pick the stuck task up and assign it on its own next tick, with no client action.

  **Boundary/negative tests**, all confirmed: a merchant with no store location gets `StoreLocationNotSetException` (400) on `mark ready`; a partner acting on a task no longer assigned to them (after a reassignment) gets 403; a customer hitting a `DELIVERY_MANAGE`-gated endpoint gets 403; no bearer token gets 401; hitting delivery-service directly on :8090, bypassing the gateway, gets 403 from `InternalRequestFilter`.

  **Two real bugs found and fixed during testing, both before this stage could be called done:**
  - `MerchantOrderService` resolved "merchantId" as `Merchant.getId()` — merchant-service's own internal application-record PK — for task lookups, ownership checks, and the `order.ready` payload. Every other service in the system (cart-service, order-service, catalog/inventory) treats "merchantId" as the store owner's **userId**, which is what `OrderConfirmedConsumer` actually stored on `MerchantOrderTask.merchantId` (straight from the event payload, itself `order.confirmed`'s `merchantId` = ownerUserId). The mismatch meant `GET /pending` always returned `[]`, and `accept`/`reject` would have thrown a false `ForbiddenException` the first time they were exercised against a real order — caught only by actually placing an order and hitting the endpoint, not by inspection. Fixed by using `ownerUserId` directly throughout `MerchantOrderService`, matching the system-wide convention.
  - `DeliveryTask.excludedPartnerIds` (an `@ElementCollection`, lazy by JPA default) threw `LazyInitializationException` the moment `rejectAndReturnToQueue()` tried to `.add()` to it — `DeliveryService` deliberately has no `@Transactional` anywhere (the same doctrine `OrderService` has followed since Stage 8), so the Hibernate session was already closed by the time the reject flow touched the collection. Only surfaced when a real reject was tested, not on the happy path (which never touches the set). Fixed by marking the collection `FetchType.EAGER` — it's small and bounded (a handful of UUIDs per task at most), so eager loading is the right call rather than reaching for a transaction boundary the rest of the service deliberately avoids.

### Stage 12 — Notification
- [x] `notification-service` (MongoDB): consume `user.registered`, `order.confirmed`, `payment.failed`, `inventory.low`
- [x] Email send (or logged-only stub if no real provider wired)
- **Done when:** each consumed event produces a logged/sent notification record. ✅ Verified end-to-end against the real running stack.

  **Straightforward this time — every producer already existed.** Unlike Stage 11, all 4 events (`user.registered`, `order.confirmed`, `payment.failed`, `inventory.low`) already had real producers from earlier stages; this was the first stage in a while with no sequencing gap to resolve first. `notification-service` is a 4th consumer group on `user-events` (alongside auth-service's own publish, payment-service's wallet auto-provisioning, and user-service's own profile creation) and single-topic consumer groups on `order-events`, `payment-events`, and `inventory-events`, each filtering to exactly one `eventType` per ARCHITECTURE.md §7's table — `order-events` also carries `order.created`/`merchant.accepted`/`merchant.rejected`/`order.ready`, `payment-events` also carries `payment.success`, `inventory-events` also carries `inventory.out`, all deliberately ignored here since the architecture table doesn't list them for this service.

  **`LoggingNotificationSender`** is the stub BUILD_TRACKER.md's own wording sanctioned ("or logged-only stub if no real provider wired") — logs at INFO as the actual "send" mechanism, behind a `NotificationSender` interface so a real email/SMS provider is a new implementation later, not a redesign. `NotificationService.record()` is idempotent on the source Kafka event's own `eventId` (checked via `existsByEventId` before inserting) rather than a separate `processed_events` table, per ARCHITECTURE.md §11 — for a service whose entire job is "did we already log this," the notification record itself doubles as its own idempotency ledger.

  **Verified without writing a single new test event**, because `auto-offset-reset: earliest` (deliberate, same reasoning as delivery-service's Stage 11 choice — a new consumer group should backfill everything already on these topics rather than silently start blind) meant the very first boot processed the *entire* existing history: 63 notifications materialized immediately — 38 `user.registered`, 14 `order.confirmed`, 9 `inventory.low`, 2 `payment.failed` — with `payload.get(...)`-derived content that read correctly against real historical orders/products, and confirmed via `db.notifications.distinct("eventType")` that only those exact 4 types ever got a record (no `order.created`, `payment.success`, or `inventory.out` leaked through the per-consumer filters). A fresh live order placed afterward produced a fifth `order.confirmed` notification with the correct `recipientUserId`, confirming the backfill wasn't masking a live bug in the current payload shape.

  **Idempotency proven directly**, not just asserted: republished a real, already-processed `user.registered` event to `user-events` with the identical `eventId` — document count stayed at 64, no new `LoggingNotificationSender` log line, confirming `existsByEventId` short-circuits before either the "send" or the insert.

  **DLQ test, matching Stage 10's standard**: a hand-crafted `user.registered` event with a non-string `userId` (a nested object, so the payload-parsing cast fails deterministically) retried 3 times then landed on `user-events-notification-service-user-dlt`, logged by `UserRegisteredConsumer`'s own `@DltHandler`. The same malformed event was also broadcast to `user-events`' two *other* real consumer groups — payment-service's wallet auto-provisioner and user-service's own profile consumer — and both failed independently, landing on their own correctly-named DLTs (`user-events-payment-service-dlt`, `user-events-user-service-dlt`) with no cross-contamination, reconfirming Stage 10's service-qualified-suffix fix holds under a genuinely new third consumer group on an existing topic, exactly the scenario that fix was written to prevent from recurring.

  **`GET /api/notifications/mine`** (self-scoped via `X-User-Id`, same pattern as `OrderController.mine` — no separate RBAC permission needed for a caller reading their own data) confirmed newest-first and correctly scoped; no bearer token → 401; hitting notification-service directly on :8091, bypassing the gateway, → 403 from `InternalRequestFilter`.

### Stage 13 — Review
- [x] `review-service` (MongoDB): create review, gated by "has this user ordered this product" via Feign to order-service
- [x] Product rating aggregation endpoint
- **Done when:** a review can only be created after a delivered order containing that product; rating aggregate updates correctly. ✅ Verified end-to-end against the real running stack.

  **"Via Feign" per the checklist, via `@LoadBalanced RestTemplate` in practice** — this project has never actually used Feign anywhere; every prior internal service-to-service call (cart→inventory, order→coupon/inventory/payment/cart, inventory→catalog) uses the same load-balanced-`RestTemplate`-plus-internal-secret-interceptor pattern, so `review-service`'s new `OrderClient` follows that established convention rather than introducing a second HTTP client abstraction into the codebase for one call.

  **New internal endpoint on order-service**: `GET /api/orders/internal/delivered?userId=&productId=`, reusing `/api/orders`'s existing base path with a 2-segment sub-route (matching `InternalPaymentController`'s and `ReservationController`'s exact precedent — internal endpoints share their service's normal path prefix, gated purely by `InternalRequestFilter`'s `X-Internal-Secret` check, no separate "/internal-only" URL convention needed). Backed by a single derived query, `existsByUserIdAndStatusAndItems_ProductId(userId, DELIVERED, productId)`, traversing the existing `Order → OrderItem` JPA relation — no new table, no denormalization.

  **Duplicate-review prevention is a real unique index, not just a check-then-insert**: `Review` has a MongoDB compound unique index on `(userId, productId)`. `ReviewService.create()` still pre-checks with `existsByUserIdAndProductId` for a clean 409 on the common path, but the index is what actually closes the race window between that check and the save — caught via `DuplicateKeyException` and translated to the same `ReviewAlreadyExistsException`. Mongo's `auto-index-creation` defaults to **off** in Spring Data MongoDB (confirmed catalog-service's own `@Indexed(unique = true)` fields from Stage 4 never explicitly enabled it either, an existing gap not touched here), so this was set explicitly for review-service — verified directly via `db.reviews.getIndexes()` that the real unique index exists, not assumed from the annotation alone.

  **Rating aggregation computed live**, not stored: a MongoDB aggregation pipeline (`$match` on `productId`, `$group` with `$avg`/`$count`) runs on every `GET .../rating` call rather than maintaining a running average that could drift from the underlying reviews — consistent with ARCHITECTURE.md's own characterization of this collection ("read-heavy... no cross-entity transactions").

  **Full flow verified live**: a fresh customer with zero orders got a 403 (`ReviewNotEligibleException`) attempting to review a real product; the same customer was walked through a full order (wallet top-up → cart → checkout → merchant accept/ready → delivery-service assignment → partner accept/pickup/deliver, reusing Stage 11's merchant/partner accounts) to a genuine `DELIVERED` status; the identical review request that had 403'd now succeeded (201); a second identical request 409'd (`ReviewAlreadyExistsException`); a second, independent reviewer with a delivered order from **Stage 11's own testing** (days-old data, not created fresh for this test) successfully reviewed the same product, and the rating aggregate correctly read `{averageRating: 4.0, reviewCount: 2}` for ratings of 5 and 3. A product with zero reviews correctly returned `{averageRating: 0.0, reviewCount: 0}` rather than an error or a NaN. Validation (`rating` outside 1–5 → 400), auth boundaries (no token → 401 on both `POST` and `/mine`), and gateway-bypass (direct hits on both review-service's :8092 and order-service's new internal endpoint on :8089 → 403 without `X-Internal-Secret`) all confirmed. No bugs found this stage.

### Stage 14 — Settlement
- [x] `settlement-service` (Postgres): SettlementEntry created on `order.delivered`
- [x] Scheduled batch job aggregates entries → Payout
- [x] Simulated payout execution (PENDING → PROCESSING → PAID)
- **Done when:** a delivered order produces a settlement entry with correct commission math, batch job correctly groups entries per merchant. ✅ Verified end-to-end against the real running stack.

  **Sequencing gap resolved first, again**: `order.delivered` had never actually been published — order-service's `delivered()` method (Stage 11) reached the terminal `DELIVERED` status but stopped there, with no event describing that fact for anyone downstream. Added `OrderEventPublisher.publishDelivered(orderId, merchantId, totalAmount)`, fired the same way as every other terminal-ish transition in this service (save first, publish the saved state after — never inside a transaction).

  **Commission math matches ARCHITECTURE.md §4.7's own worked example exactly**: 5% commission rate (configurable, `vicinia.settlement.commission-rate`), `BigDecimal` with `RoundingMode.HALF_UP` at scale 2 (the same convention coupon-service's discount math used in Stage 7). Verified against a real ₹40.00 order: commission ₹2.00, net ₹38.00 — the same 5% ratio as the architecture doc's ₹500/₹25/₹475 example, confirmed via a live delivered order, not just unit arithmetic.

  **The one deliberate exception to this project's no-`@Transactional`-in-orchestration doctrine**: `SettlementService.runBatch()` uses `@Transactional`, and says so explicitly in its own Javadoc why — every prior service's avoidance of `@Transactional` (order-service since Stage 8, delivery-service/merchant-service in Stage 11) exists specifically to keep an external call (a Kafka publish, another service's REST call) out of a transaction boundary that might still roll back. `runBatch()` makes no external call at all — it's a single atomic multi-row Postgres write, creating one `Payout` and linking every one of a merchant's `PENDING` `SettlementEntry` rows to it — exactly the case `@Transactional` exists for, and the one place in this codebase so far where the doctrine's own stated reason doesn't apply.

  **Two independent scheduled jobs, on the same reaper pattern as Stage 5's `ReservationReaper` and Stage 11's `AssignmentReaper`**: `PayoutBatchJob` (real cadence: daily, per the architecture doc's own "e.g. daily") groups `PENDING` entries into `Payout`s; `PayoutProcessor` (30s tick) advances `PENDING → PROCESSING` immediately and `PROCESSING → PAID` once a configurable processing delay has passed (60s in dev), publishing `settlement.completed` at the moment of the real transition — not a mock event fired eagerly on `Payout` creation. Both jobs also have on-demand admin endpoints (`POST /admin/payouts/run-batch`, `/run-processor`) for ops/testing without waiting on the schedule.

  **Verified without touching any manual trigger for the processor**: a real order was walked through checkout → merchant accept/ready → delivery assignment → partner accept/pickup/deliver → `DELIVERED`, producing a real `SettlementEntry` (`PENDING`, ₹40.00/₹2.00/₹38.00). Admin `run-batch` grouped it into a `Payout` (₹38.00 — the net, not the gross, since that's what the merchant actually receives). Left alone, the automatic scheduled `PayoutProcessor` advanced it `PENDING → PROCESSING → PAID` entirely on its own within the configured ~60s window, with no admin action — confirmed both by polling the DB and by finding a genuine `settlement.completed` message on `settlement-events` with the correct payout ID, merchant ID, and amount.

  **Idempotency and DLQ, matching Stage 10/12/13's standard**: republishing the identical `order.delivered` event (same `orderId`, different hand-crafted `eventId` — a stronger test than eventId-only dedup, since it also protects against two genuinely different messages referencing the same order) produced no duplicate entry, confirming the idempotency check is keyed on the actual business fact (`existsByOrderId`) rather than just message identity. A malformed event (`totalAmount: "not-a-number"`, failing `BigDecimal`'s parse deterministically) retried 3 times and landed on `order-events-settlement-service-dlt`, logged by its own `@DltHandler`, with zero side effects on the real ledger (entry count unchanged). RBAC (`SETTLEMENT_MANAGE`, newly seeded onto `ADMIN`) confirmed: a merchant hitting an admin endpoint gets 403; no token gets 401; hitting settlement-service directly on :8093, bypassing the gateway, gets 403 from `InternalRequestFilter`. No bugs found this stage.

### Stage 15 — Resilience Hardening
- [x] Circuit breaker: order-service → payment-service (Razorpay path)
- [x] Retry (idempotent-only): order-service → inventory-service, order-service → payment-service
- [x] Timeout on every Feign client
- [x] Bulkhead: Razorpay calls isolated from internal-service calls
- [x] Gateway rate limiting (Redis token bucket)
- **Done when:** killing the Razorpay-dependent path degrades gracefully without exhausting threads needed for wallet-only checkout. ✅ Verified end-to-end against the real running stack.

  **"Every Feign client" is this project's RestTemplate clients** — as noted in Stage 13, Feign has never actually been used anywhere here; every internal call goes through a `@LoadBalanced RestTemplate`. Added explicit connect/read timeouts (`SimpleClientHttpRequestFactory`, 2s/5s) to all 4 services with one: order-service, cart-service, inventory-service, review-service.

  **Retry and circuit-breaker+bulkhead deliberately split across two different libraries already present in this project**, rather than picking one for everything: `@Retryable` (Spring Retry — already a dependency since Stage 10's `@RetryableTopic`) on `InventoryClient.reserve()` and `PaymentClient.payWithWallet()`, scoped to `ResourceAccessException`/`HttpServerErrorException` only (never the 402/409 business rejections those methods already handle); `resilience4j-spring-boot3` (new dependency, not managed by spring-cloud-dependencies) for `@CircuitBreaker`+`@Bulkhead` on `PaymentClient.createRazorpayOrder()` only — retry was deliberately *not* applied there, since creating a Razorpay order isn't idempotent to blindly retry (a naive retry mints a second, distinct Razorpay order).

  **A real config-precedence bug found and fixed along the way**: order-service's own `management.endpoints.web.exposure.include` (widened to add `circuitbreakers`/`circuitbreakerevents`) was silently overridden by Config Server's shared `application.yml`, which sets the same key to a narrower value — Spring Cloud Config's remote property source takes precedence over a service's own local file once `spring.config.import` pulls it in. Fixed via `config-repo/order-service.yml`, the first per-service override this project has actually needed, exactly the mechanism `application.yml`'s own top-of-file comment predicted ("Per-service overrides go in `{spring.application.name}.yml`").

  **Circuit breaker's full lifecycle proven live**, not just configured: temporarily pointed `RazorpayClient`'s hardcoded URL at an unreachable host, rebuilt+restarted payment-service only (wallet path untouched — genuinely isolates "Razorpay degraded" from "payment-service down," the actual real-world scenario this pattern protects against). Fired repeated Razorpay-path checkouts and watched `/actuator/circuitbreakers` transition `CLOSED → OPEN` once the failure-rate threshold was crossed (`notPermittedCalls` climbing — calls rejected without even attempting the network call), confirmed `automatic-transition-from-open-to-half-open-enabled` correctly moved it to `HALF_OPEN` after the wait duration, watched it reopen when trial calls still failed (URL still broken), then reverted the URL and watched 3 successful trial calls in `HALF_OPEN` close it back to `CLOSED`. The entire state machine — not just one transition — verified against the real running JVM.

  **The isolation claim itself proven, not assumed**: while the razorpay circuit sat `OPEN`, a wallet order with sufficient balance completed in 0.25s at `CONFIRMED` — completely unaffected. Razorpay-path attempts during the same window failed in well under 0.1s each (`CallNotPermittedException`, no network I/O attempted), returning a clean 503 via the new `PaymentGatewayUnavailableException`.

  **Retry proven with a genuine transient failure, not just a code read**: `kill`ing a service's Maven-wrapper PID triggers a graceful Eureka deregistration (`IllegalArgumentException: Service Instance cannot be null` — a load-balancer-level failure, correctly *not* retried, since no instance existing means retrying immediately can't help). `kill -9`ing the actual child JVM process instead leaves Eureka still reporting the instance `UP` while its port is genuinely dead — the real "transient network blip" scenario retry exists for. A wallet order against that state took 1.68s (matching 3 attempts across the configured 500ms/1000ms backoff) before failing with `ResourceAccessException: ... Connection refused` — exactly the retryable exception type, confirmed in the logs. `reserve()`'s retry uses the identical pattern and was verified by code review rather than an independently isolated live test, since cart-service's own (unprotected, out of this stage's scope) dependency on inventory-service fails first when that service is down, making a clean isolated test of `reserve()` alone impractical without killing cart-service's own call path too.

  **Gateway rate limiting confirmed with real concurrency**, not sequential requests (which let the bucket refill between calls and never trip): 80 truly concurrent requests (`xargs -P 40`) against an authenticated endpoint produced 60×200/20×429, matching the configured 20 req/s replenish + 40 burst; a different user's request succeeded immediately (200) while the first user's bucket was still exhausted, confirming per-user `KeyResolver` isolation; the same concurrent-burst test against a public, unauthenticated endpoint produced a clean 40×200/40×429 split, confirming the IP-based fallback for paths with no `X-User-Id` yet.

  **One real, pre-existing gap surfaced by this stage's own testing, left unfixed as out of scope**: when `createRazorpayOrder` fails (circuit open, bulkhead full, or any other reason), the order is left at `PAYMENT_PENDING` with inventory still reserved — nothing releases the reservation or marks the order failed, since no catch ever existed around that call. This predates Stage 15 (any Razorpay failure, resilience-pattern-triggered or not, hits the same gap) and isn't what this stage's own "degrades gracefully" bar is about — that bar is satisfied (fast failure, clear error, wallet checkout unaffected) — but it's a real order-lifecycle correctness issue worth a future stage's attention, not silently patched here.

### Stage 16 — Observability
- [x] Correlation ID: generated at gateway, propagated via header + into Kafka headers, MDC logging
- [x] Micrometer → Prometheus on every service
- [x] Grafana dashboards: order funnel, inventory-oversell counter (should be zero), Kafka consumer lag, payment success/failure rate, stale READY_FOR_PICKUP orders
- **Done when:** a single order's correlation ID can be traced across logs in ≥4 different services, and all 5 dashboards render real data. ✅ Verified end-to-end against the real running stack.

  **Correlation ID split across two mechanisms, deliberately**: a servlet `CorrelationIdFilter` (new, in `common`) for the 13 MVC domain services — reads `X-Correlation-Id`, puts it in MDC, echoes it on the response, cleans up in a `finally` block since servlet threads are pooled and reused. api-gateway is WebFlux, where traditional ThreadLocal-based MDC doesn't reliably follow a request across its event-loop hops, so it gets its own `CorrelationIdGlobalFilter` instead — generates/propagates the header and logs one explicit line per request, deliberately not attempting full Reactor-Context MDC propagation for gateway's own logs, since "propagate to every downstream service" (the actual cross-service requirement) doesn't need it.

  **Into Kafka headers, literally — not a payload field**: a `ProducerInterceptor` (raw Kafka SPI, configured purely via `spring.kafka.producer.properties.interceptor.classes` in each service's `application.yml` — zero publisher code touched) copies the current MDC value onto every outgoing record as a real header. A Spring Kafka `RecordInterceptor`, registered via a `ContainerCustomizer` bean in each Kafka-consuming service's `SecurityBeansConfig`, reads it back and re-sets MDC immediately before each record reaches its `@KafkaListener` method — the Spring-native hook, not the raw `ConsumerInterceptor` SPI, since that one only sees whole batches before Spring's container dispatches individual records, which would get MDC timing wrong for a multi-record batch.

  **A second config-precedence bug found and fixed**, on top of Stage 15's: api-gateway's own local `application.yml` has set `management.endpoints.web.exposure.include: health,info,gateway` since Stage 1 — silently overridden by Config Server's shared `application.yml` the whole time (the exact bug fixed for order-service in Stage 15), meaning the `/actuator/gateway` endpoint has likely never actually been reachable. Fixed with `config-repo/api-gateway.yml`, folding in `prometheus` alongside the restored `gateway` endpoint.

  **`micrometer-registry-prometheus` added once, to `common`'s own pom** — every one of the 14 services that already depends on `common` gets a working `/actuator/prometheus` endpoint the moment `prometheus` is added to their exposure list, without touching 14 separate poms. `discovery-server` and `config-server` (the two services with no `common` dependency) get it declared directly, since neither can import from Config Server to receive a shared override anyway (config-server *is* the thing serving that override; discovery-server is what config-server registers with).

  **Custom business metrics, each with a real reason for where it's instrumented**:
  - Order funnel (`order_funnel_total{stage}`) — one counter, incremented at the 4 exact points in `OrderService` (`created`, `payment_success`, `merchant_acceptance`, `delivered`) rather than derived from Kafka event counts, since those 4 points are the actual business moments the dashboard describes.
  - Inventory oversell (`inventory_oversell_total`) — registered *eagerly* in `InventoryService`'s constructor, not lazily on first increment (Micrometer's usual `meterRegistry.counter(...).increment()` pattern) — a "should always read zero" canary that's simply *absent* from Prometheus until the bug it watches for happens once would be indistinguishable from "no data yet," defeating the whole point.
  - Payment success/failure (`payment_outcome_total{method,outcome}`) — instrumented once, inside `PaymentEventPublisher.publishSuccess`/`publishFailed`, which are already the single call site both `WalletService` and `RazorpayPaymentService` converge on for all 4 real success/failure events — no need to touch either service's own logic.
  - Stale `READY_FOR_PICKUP` orders (`order_stale_ready_for_pickup`) — a pull-based Gauge backed by one new repository query (`countByStatusAndUpdatedAtBefore`), re-evaluated on every Prometheus scrape; no scheduled job needed, unlike this project's reaper pattern elsewhere, since this metric only needs to be *read*, never acted on.
  - Kafka consumer lag needed zero application code — Spring Boot's own Kafka+Micrometer integration auto-registers `kafka_consumer_fetch_manager_records_lag` for every `@KafkaListener` consumer group the moment `micrometer-registry-prometheus` is on the classpath.

  **A real Docker volume-mount bug found and fixed during Grafana provisioning**: mounting the dashboard JSON subdirectory as a *second*, separate read-only volume (nested inside the already-mounted, also-read-only `provisioning/` parent) failed outright — Docker can't create a mountpoint inside a directory a different volume has already mounted read-only. Fixed by moving the dashboard JSON files into the existing `provisioning/dashboards/json/` tree and mounting the whole `provisioning/` directory once.

  **Verified live, not just wired**: fired a request with a hand-set `X-Correlation-Id` through a real checkout (signup → wallet top-up → cart → order) and found the identical ID in **8** services' logs, not just the required 4 — including two Kafka-consumer hops (merchant-service's `OrderConfirmedConsumer`, notification-service's listeners), proving both the HTTP-header and Kafka-header propagation paths work, not just one. All 16 Prometheus scrape targets confirmed `up`. All 5 Grafana dashboards confirmed loaded via the API and queried directly against Prometheus with each panel's exact PromQL, all returning real, non-empty data: 4 funnel stages with real counts, oversell canary reading a genuine `0`, 72 real Kafka-lag series, a 100% WALLET success rate, and a genuine `0` stale-order count. Confirmed the new correlation-ID filter doesn't interfere with the existing gateway-bypass 403 rejection.

### Stage 17 — Testing Hardening Pass
- [x] Unit tests: state-machine transitions (order, merchant onboarding), reservation math
- [x] Integration tests (Testcontainers): each service against its real DB
- [x] Kafka tests (Testcontainers): producer shape, idempotent consumer, DLQ path
- [x] One full e2e test: checkout → confirm → merchant accept → delivery → deliver → settlement
- **Done when:** the e2e test passes in CI, not just locally. ✅ `.github/workflows/ci.yml`, two jobs (`test`: unit + Testcontainers integration across the whole reactor; `e2e`: `start-infra.sh` + the new `e2e-tests` module against the real running stack), both green on GitHub Actions across two consecutive runs.

  4 unit test classes (order/merchant/delivery/settlement status-machine transitions, full-matrix `@ParameterizedTest`), 5 Testcontainers repository integration tests (inventory oversell concurrency, wallet idempotency concurrency, order derived queries, category + review uniqueness), 1 Kafka integration test (producer shape, idempotent consumer via redelivery, DLQ path), 1 e2e test (`backend/e2e-tests`, plain `java.net.http.HttpClient` + JDBC, no Spring/Testcontainers) driving the real checkout → confirm → merchant accept → delivery → deliver → settlement flow over HTTP against a live stack.

  **A real production bug found while writing these tests**: catalog-service never set `spring.data.mongodb.auto-index-creation`, so `Category`'s unique index on name/slug had never actually been enforced since Stage 4 — the same gap review-service had in Stage 13, explicitly noted-but-left-alone at the time. Fixed.

  **Local Testcontainers execution was blocked the whole time** by a genuine Docker Desktop 4.87.0 / docker-java version incompatibility on this developer machine (confirmed unrelated to Docker's health after a clean restart, and unresolved across 3 different Testcontainers versions) — every Testcontainers-dependent test was written and compile-verified locally (`mvn test-compile`) but actually *executed*, for the first time ever, only in CI. That first real run surfaced 5 genuine bugs no amount of local `test-compile` could have caught:
  - Inventory's oversell concurrency test: a bare custom `@Modifying @Query` method invoked from worker threads isn't reliably auto-wrapped by Spring Data's repository proxy (spring-data-jpa#1420/#3237/#3733) — fixed with an explicit `TransactionTemplate` per worker, mirroring `InventoryService.reserve()`'s real `@Transactional` boundary.
  - The identical bug in wallet's idempotency concurrency test — same fix.
  - The Kafka test's redelivery case: `OrderService.confirmFromPaymentEvent` calls out to inventory-service over real HTTP, which nothing in that test starts — every attempt threw, retried 4 times via `@RetryableTopic`, and landed on the DLT, which also broke the DLQ test's own record count. Fixed with `@MockBean InventoryClient`.
  - The Kafka test's 3 methods also weren't isolated from each other (one shared consumer group) — the DLQ test's ~14s retry backoff could starve the redelivery test depending on JUnit's method execution order. Fixed with explicit `@Order`.
  - `ReviewRepositoryIntegrationTest`'s 4 methods all hardcoded `"user-1"`/`"product-1"` — `@DataMongoTest` gets no transactional rollback between methods the way `@DataJpaTest`'s Postgres does, so whichever method ran after another had already inserted that pair hit a real `DuplicateKeyException` on its own unrelated first save. Fixed with UUID-unique data per test.

  Also found and fixed two CI-environment-only gaps unrelated to application code: the pinned Testcontainers version's `KafkaContainer` didn't recognize the native `apache/kafka` image (switched to `confluentinc/cp-kafka`, what that version actually supports), and the Testcontainers job's `@SpringBootTest`-based Kafka test needed `VICINIA_INTERNAL_SECRET`/`VICINIA_JWT_SECRET` supplied directly (normally sourced from config-server, which that job never starts).

### Stage 18 — Frontend
- [x] Customer app: browse, cart, checkout, order tracking, profile
- [x] Merchant dashboard: onboarding status, listings, order queue
- [x] Delivery agent app: assignment, status updates, live location ping
- [x] Admin dashboard: approvals, order/payment visibility, coupon management
- **Done when:** all 4 role-specific flows work against the real backend (not mocked). ✅ Verified live against a real `start-infra.sh` stack (Playwright walkthrough, not just manual clicking): fresh signups for all 4 roles, real product search → cart → WALLET checkout → order placed and tracked through its status timeline, a merchant applying → admin-approving → going live → creating a real listing through the UI, and an admin approving from the same pending queue. Both light and dark themes screenshotted at every step.

  **Architecture**: one React app (`frontend/customer-app`, kept as the existing project's directory — this *is* the full Stage 18 build now, not the placeholder README's old "click through what's built" scope), role-gated by route prefix (`/merchant`, `/delivery`, `/admin`; customer routes unprefixed) via a new `RoleRoute` guard that redirects a logged-in user away from another role's area to their own home rather than a 403. One shared design system throughout (existing `tokens.css`, `Icons.jsx`, `Illustrations.jsx` — extended, not replaced): the same Blinkit-inspired yellow accent, card/badge/modal patterns, and three-state light/dark theming already established in Stage 3, now covering roughly 3x the screens.

  **A real, honest backend gap found while building the delivery app**: there is no "list my assigned delivery tasks" endpoint, and notification-service has no delivery-assignment consumer — a partner has no server-side signal that a task exists. Rather than fake it, `DeliveryHomePage` is built around what's actually there (online/offline + location ping, and per-task accept/picked-up/delivered actions reachable by orderId), with the gap documented in `utils/deliveryHistory.js`'s own comment rather than papered over.

  **New shared pieces**: `CartContext` (mirrors the existing `AuthContext` pattern) so the navbar's live cart badge and the cart/checkout pages share one source of truth; `StatusBadge` + `utils/status.js` mapping every one of this project's `*Status` enums (order, merchant, order-task, delivery-task, settlement, payout) to a consistent tone; `ProductImage` with an `onError` fallback, needed because seed/test data's `example.com` image URLs 404 — found via the first real screenshot, not assumed.

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
