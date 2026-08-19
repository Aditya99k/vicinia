# ADR 0002: Inventory Database & Concurrency Control

**Status:** Approved

## Context

The oversell problem (stock=1, two concurrent buyers) needs a correctness guarantee. Options considered: MongoDB with app-level locking, Redis distributed locks, or PostgreSQL atomic conditional updates.

## Decision

PostgreSQL, with atomic conditional `UPDATE`:

```sql
UPDATE merchant_listing
SET available_stock = available_stock - :qty,
    reserved_stock   = reserved_stock + :qty
WHERE listing_id = :id AND available_stock >= :qty;
```

No distributed lock service.

## Consequences

- Correctness guarantee comes from the database natively — no new failure mode (lock service unavailable, lock not released on crash, TTL mismatch).
- `inventory-service` is the one exception to the general Mongo-for-read-heavy instinct elsewhere in the system — it must be Postgres.
- Stage 5 must ship a concurrency test (N parallel reservations against stock=1, assert exactly one succeeds) before anything depends on it.

See `ARCHITECTURE.md` §4.4, §6.
