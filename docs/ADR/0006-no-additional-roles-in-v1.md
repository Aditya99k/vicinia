# ADR 0006: Roles Beyond the Core 4

**Status:** Approved

## Context

Merchant staff (cashier vs. owner logins) and a support-agent role were considered for V1.

## Decision

No new roles or services now. RBAC is built as role + permission set (not just a role string) from day one, so `SUPPORT_AGENT`/`MERCHANT_STAFF` can be added later as scoped permission bundles without a redesign.

## Consequences

- No V1 payoff for building these now; real RBAC-dimension complexity cost avoided.
- `auth-service`'s RBAC model must support permission bundles, not just role strings, from the first implementation — this is the one place the "defer" decision still constrains Stage 2's design.

See `ARCHITECTURE.md` §2.
