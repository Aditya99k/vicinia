# ADR 0005: Settlement Service Boundary

**Status:** Approved

## Context

Settlement (merchant payout ledger) could be folded into `payment-service` or kept as its own service.

## Decision

Separate service from the start, but last in build order (Stage 14).

## Consequences

- Different cadence (real-time capture vs. batch payout), different data shape (ledger vs. transaction), different future integration surface (bank payout API) — clean boundary justifies the split.
- One more service to stand up, but nothing else in the checkout path blocks on it, so it doesn't hold up earlier stages.

See `ARCHITECTURE.md` §4.7, §7.
