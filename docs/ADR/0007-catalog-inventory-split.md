# ADR 0007: Catalog vs. Inventory Service Split

**Status:** Approved

## Context

Product catalog (global, canonical) and merchant listings (per-merchant price/stock) could be modeled as one service or two.

## Decision

Keep split: `catalog-service` (global product, MongoDB) and `inventory-service` (per-merchant listing, PostgreSQL).

## Consequences

- Different databases, different read/write ratios, different consistency needs — the clearest "genuinely different bounded contexts" case in the system.
- Two services instead of one, but each is simpler and independently scalable.

See `ARCHITECTURE.md` §4.2, §6, §7.
