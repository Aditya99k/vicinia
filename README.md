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

## Local Dev

First time only — every secret is read from `.env` (gitignored, never committed):

```
cp .env.example .env
# then fill in VICINIA_INTERNAL_SECRET and VICINIA_JWT_SECRET, e.g.
python3 -c "import secrets; print(secrets.token_urlsafe(32))"   # -> VICINIA_INTERNAL_SECRET
python3 -c "import secrets; print(secrets.token_urlsafe(48))"   # -> VICINIA_JWT_SECRET
```

Then:

```
./start-infra.sh   # loads .env, infra + every backend service + the frontend, in dependency order
./stop-infra.sh    # tears it all back down
```

`start-infra.sh` refuses to start if `.env` is missing or incomplete, and is otherwise idempotent — safe to re-run if some services are already up, it'll skip what's already running. Logs land in `./logs/<service>.log`.

**Maintenance:** when a new backend module is scaffolded, add one line to the `SERVICES` array near the top of `start-infra.sh`, in dependency order — and if it introduces a new secret, add it to both `.env.example` and `REQUIRED_ENV_VARS`. `stop-infra.sh` needs no changes — it stops whatever's tracked in `.pids/`.

### Frontend (validation only)

`frontend/customer-app` is a real React app for clicking through what's built so far — not the full Stage 18 build. `start-infra.sh` brings it up automatically (installing dependencies on first run) at `http://localhost:5173`, and prints a LAN URL too for testing from a phone on the same Wi-Fi. See its own [README](frontend/customer-app/README.md).

## Status

Stage 12 (notification) — see `docs/BUILD_TRACKER.md` for current progress.
