# Vicinia

Hyperlocal, multi-merchant commerce platform. Independent local merchants self-onboard, run their own store, and fulfill orders placed against their store specifically — not a platform-wide warehouse.

V1 loop: merchant onboards → customer discovers a nearby store → places an order from that one store → payment → merchant prepares → delivery partner delivers → merchant gets settled.

## Docs

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — product scope, service boundaries, data/event design, all approved architecture decisions
- [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) — zero-cost deployment plan (Oracle Free VM, Redpanda, self-hosted Postgres/Redis, Atlas, Cloudflare)
- [`docs/BUILD_TRACKER.md`](docs/BUILD_TRACKER.md) — stage-by-stage build checklist, the working document for tracking progress
- [`docs/ADR/`](docs/ADR/) — one ADR per architecture decision

## Structure

```
backend/    Spring Boot microservices (Maven multi-module)
frontend/   React apps (customer, merchant, delivery, admin)
docs/       Architecture, deployment, build tracker, ADRs
```

## Status

Stage 0 (foundations) — see `docs/BUILD_TRACKER.md` for current progress.
