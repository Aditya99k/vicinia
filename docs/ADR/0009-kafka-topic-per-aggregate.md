# ADR 0009: Kafka Topic Granularity

**Status:** Approved

## Context

Topic granularity could be one topic per event type (20+ topics) or one topic per aggregate with a type field.

## Decision

One topic per aggregate: `order-events`, `payment-events`, `inventory-events`, `merchant-events`, `delivery-events`, `user-events` — each event carries an `eventType` field.

## Consequences

- Simpler ordering guarantees: Kafka only guarantees order within a partition, and all events for one order (etc.) need to be ordered relative to each other. Six aggregates, six topics, not 20+.
- Every consumer must switch on event type rather than subscribing to a narrowly-typed topic.

See `ARCHITECTURE.md` §9.
