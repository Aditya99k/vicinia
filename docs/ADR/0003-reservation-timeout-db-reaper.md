# ADR 0003: Reservation Timeout Mechanism

**Status:** Approved

## Context

Inventory reservations must be released if a payment never completes. Redis TTL/keyspace-notification eviction was considered as the trigger mechanism.

## Decision

DB-driven scheduled reaper job. Every reservation row stores `reserved_at`; a scheduled job (every 1–2 min) finds reservations older than N minutes still in `PAYMENT_PENDING` and releases them.

## Consequences

- Redis keyspace notifications are opt-in, not durable, and easy to miss under load or a Redis restart — rejected for that reason.
- The reaper is boring but reliable: driven by the database, survives restarts.
- Adds a small periodic job to `inventory-service`.

See `ARCHITECTURE.md` §4.4.
