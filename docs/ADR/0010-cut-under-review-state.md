# ADR 0010: Merchant Onboarding — Cut UNDER_REVIEW State

**Status:** Approved

## Context

Merchant onboarding could distinguish "in queue" (`PENDING_REVIEW`) from "being actively looked at" (`UNDER_REVIEW`) as separate states.

## Decision

Cut `UNDER_REVIEW` as a distinct state. Onboarding state machine is: `PENDING_REVIEW → APPROVED/REJECTED → ONBOARDING → LIVE`.

## Consequences

- No behavioral difference between the two states — the distinction was cosmetic, admin-UI-only value.
- One less state to test and handle everywhere in `merchant-service`.

See `ARCHITECTURE.md` §4.1.
