# ADR 0001: Single Merchant per Order

**Status:** Approved

## Context

An order could in principle be fulfilled by multiple merchants in one checkout (split order). Real hyperlocal apps (Swiggy Instamart, Blinkit, Zepto) mostly avoid this and force single-store carts.

## Decision

One merchant per order for V1. `cart-service` enforces exactly one `merchantId` per cart — adding an item from a different merchant either replaces the cart or prompts the user to clear it.

## Consequences

- `order-service` always has exactly one `merchantId` — never a runtime decision.
- Avoids multiplying downstream complexity: multiple deliveries/merged routes, partial payment capture, partial cancellation, multiple settlement records, multiple prep-time ETAs to reconcile.
- Multi-merchant carts are a possible V2 feature; revisit only if product requirements demand it.

See `ARCHITECTURE.md` §4.3.
