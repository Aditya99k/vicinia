# Vicinia — Zero-Cost Deployment Architecture

Status: FINAL. Not needed until Stage 19–20 of the build order (see `BUILD_TRACKER.md`) — nothing here blocks starting Stage 0/1. Recorded now so the decision doesn't drift while the backend is being built against it.

Constraint: every piece — compute, databases, message bus, cache, image storage, frontend hosting, CDN, TLS, CI/CD — must be **permanently free**, not a 30-day trial or a credit that runs out.

## 1. Facts This Plan Is Based On (as of Aug 2026)

- **Oracle Cloud "Always Free" ARM compute was cut from 4 OCPU/24GB to 2 OCPU/12GB in June 2026**, with a compliance deadline of Aug 18, 2026 for existing accounts. Permanent free tier, but half the RAM previously assumed. This is the binding constraint on the whole design. **Provision the VM early and confirm account approval / regional ARM capacity before relying on this plan** — both are documented, real provisioning risks independent of cost.
- **Upstash Kafka no longer exists** (deprecated Sep 2024, shut down Mar 2025) — "managed free Kafka" isn't an option; Kafka or a Kafka-compatible alternative must be self-hosted.
- **Upstash Redis free tier is real but stingy** (256MB, ~500K commands/month, single region) — cart + OTP + refresh tokens + blacklist + rate-limiting + delivery GEO queries would burn through that fast. Self-hosting Redis is more reliable and cheap in RAM.
- **MongoDB Atlas M0** (free forever, 512MB) — unchanged, still the right call, already proven in blinkit-clone.
- **Cloudinary free tier** for images — already proven in blinkit-clone, keep it.

## 2. Compute Strategy: One Oracle Free VM, Kafka Replaced with Redpanda

**Decision:** self-host everything compute-bound on a single Oracle Cloud Always Free ARM VM (2 OCPU / 12GB RAM) via Docker Compose — but swap Apache Kafka + Zookeeper (as used in blinkit-clone) for **Redpanda**.

Redpanda is Kafka-API-compatible — Spring Kafka producers/consumers don't change at all, same client library, same code — but ships as a single binary with no Zookeeper dependency and a much smaller memory footprint. Kafka + Zookeeper (~1.5–2GB combined) is not a good use of a 12GB budget alongside 19 JVMs when Redpanda does the same job in a few hundred MB. This is a deliberate deviation from blinkit-clone's infra, made for RAM reasons specific to Vicinia's larger service count.

### RAM budget on the 12GB VM

| Component | Count | RAM each (tuned) | Subtotal |
|---|---|---|---|
| Spring Boot services (16 domain + eureka + config + gateway = 19 JVMs) | 19 | ~220MB (`-Xmx160m -XX:+UseSerialGC`, Alpine JRE) | ~4.2GB |
| Redpanda (single-node) | 1 | ~500MB | ~0.5GB |
| PostgreSQL (self-hosted, 1 instance / 9 logical DBs) | 1 | ~250MB | ~0.25GB |
| Redis (self-hosted) | 1 | ~80MB | ~0.08GB |
| Docker + OS overhead | — | — | ~0.8GB |
| **Total** | | | **~5.8GB of 12GB** |

~6GB headroom for traffic spikes and JVM GC overhead. This is why the design fits: JVM heaps are aggressively capped per-service (small CRUD-shaped services don't need 512MB heaps each), and Kafka+Zookeeper → Redpanda specifically buys back RAM.

**Why self-host Postgres instead of a managed free tier (Neon/Supabase)?** Nine services want Postgres. Managed free tiers are scoped to one project/database with limited storage and — critically — **autosuspend on inactivity**, which means a multi-second cold start on the first request after idle. For a system where order-service makes synchronous calls to 3+ services per checkout, compounding cold starts is a real latency problem. Self-hosting one Postgres instance with 9 logical databases costs ~250MB (already budgeted), has no autosuspend, and still satisfies "each service owns its own database" — that principle is about schema/access ownership, not physically separate servers.

**Why keep MongoDB on Atlas instead of also self-hosting?** Only 3 services need it (catalog, notification-log, review) and Atlas M0 is free-forever, managed, backed up, and doesn't compete for VM RAM. The one piece where "managed free" beats "self-hosted" outright.

**Single point of failure, stated plainly:** one VM going down takes the entire backend down. Accepted tradeoff of zero cost — real redundancy costs real money. Fallback if Oracle capacity/approval is a problem: a Google Cloud `e2-micro` Always Free VM (1 vCPU/1GB, different regions) as a second small node — not enough alone to replace the Oracle VM, but useful to peel off 2–3 low-traffic services (notification, review) if RAM pressure becomes real.

## 3. Data Layer — Final Placement

| Service | Database | Where it lives | Why |
|---|---|---|---|
| auth, user, merchant, inventory, coupon, order, payment, delivery (tasks), settlement | PostgreSQL | Self-hosted on Oracle VM, 1 instance / 9 databases | No autosuspend, no per-project storage cap, RAM cost already budgeted |
| catalog, notification-log, review | MongoDB | Atlas M0 (managed, free forever) | Genuinely free, zero RAM cost |
| cart, OTP, refresh tokens, blacklist, rate limiting, delivery live-location (GEO) | Redis | Self-hosted on Oracle VM | Managed free tiers (Upstash) cap commands/month too low for real traffic |
| Kafka topics | Redpanda (Kafka-protocol-compatible) | Self-hosted on Oracle VM | Only realistic zero-cost event bus after Upstash Kafka's shutdown; API-compatible, no app code changes |
| Product/user images | Cloudinary | Managed, free tier | Already proven in blinkit-clone |

## 4. Frontend + CDN

**Decision: Cloudflare Pages** for the React/Vite build. Free, unlimited bandwidth (unlike Vercel's Hobby-tier 100GB/month cap), global CDN included, free `*.pages.dev` subdomain if no custom domain owned.

**Decision: Cloudflare Tunnel** in front of the API Gateway on the Oracle VM, not a raw exposed IP:port. Free, gets: a stable public hostname without opening inbound firewall ports directly, free TLS termination, and Cloudflare's edge as a CDN/cache layer for cacheable public GET traffic + basic DDoS absorption.

If no domain owned: a free wildcard-DNS service like `sslip.io` (e.g. `api.<your-ip>.sslip.io`) gives a real hostname for TLS without buying a domain. A real domain (~$10/year) is optional polish, the one place "zero cost" and "professional-looking demo" are slightly in tension.

## 5. CI/CD

GitHub Actions, free tier (2,000 min/month private, unlimited public). Pipeline: merge to `main` → build each changed service's JAR → build Docker images → push to GitHub Container Registry (free, unlimited for public repos) → SSH into the Oracle VM → `docker compose pull && docker compose up -d` for changed services only. No Kubernetes/k3s needed — explicitly deferred (Stage 20), doubly so now the whole backend fits on one VM.

## 6. Watch List — What Could Quietly Start Costing Money

None of these charge automatically, but they're where a real production spike hits a wall first:

- **MongoDB Atlas M0:** 512MB storage cap — the tightest storage ceiling in the stack, worth monitoring.
- **Oracle VM:** 200GB block storage, 10TB/month egress — generous, unlikely to bottleneck, but the 12GB RAM ceiling is real; redo the RAM math in §2 before adding services beyond the 16 planned.
- **Cloudflare Tunnel/Pages:** genuinely unlimited for this use case, lowest risk item.
- **Oracle account approval / regional ARM capacity:** not a cost risk, but a real provisioning risk — new accounts have been documented getting flagged, or a region having no available A1 capacity. Provision early, before depending on it for a deadline.

## 7. Summary

All 10 architecture decisions in `ARCHITECTURE.md` §19 are approved, and this deployment plan is locked in — not "TBD providers," these are the actual services to sign up for when Stage 19–20 of the build order comes around. None of this deployment tooling is needed until then.
