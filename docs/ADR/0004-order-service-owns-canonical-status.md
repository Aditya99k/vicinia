# ADR 0004: Order Status Ownership

**Status:** Approved

## Context

Order status transitions are triggered by three different services (order, merchant, delivery). The customer-facing "track my order" screen needs a read path. Considered: live API composition across order + merchant + delivery on every read.

## Decision

`order-service` holds the single canonical `status` field for the entire order lifecycle. Merchant-service and delivery-service manage their own internal task state, but publish Kafka events (`merchant.accepted`, `order.ready`, `delivery.assigned`, `delivery.delivered`) that order-service consumes to update `order.status`.

## Consequences

- Live API composition would turn a read into 3 network calls with 3 failure modes per page load, for data that doesn't need millisecond freshness — rejected.
- Trades a few hundred ms of eventual consistency for a single fast query against one service.
- `order-service` must consume merchant/delivery Kafka events and apply idempotent transitions (state-check-before-apply).

See `ARCHITECTURE.md` §4.5.
