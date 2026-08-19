# ADR 0008: Service-to-Service Authentication

**Status:** Approved

## Context

Internal service calls need authentication. Considered: mTLS or OAuth2 client-credentials vs. a shared internal-secret header (the pattern already proven in blinkit-clone).

## Decision

Shared `X-Internal-Secret` header pattern, reused from blinkit-clone, for V1.

## Consequences

- Proven, low-complexity, adequate at this scale.
- mTLS/OAuth2 client-credentials is the more correct production answer but adds meaningful operational complexity (cert rotation, per-service identity) for a benefit that mostly matters once there are services you don't fully trust or control.
- Documented here as a known future hardening step, not a gap the team is unaware of.

See `ARCHITECTURE.md` §14.
