package com.vicinia.cartservice.messaging;

import java.util.Map;

/** cart-service's own local view of order-service's wire shape (ARCHITECTURE.md §9) — not a shared Java type, same reasoning as user-service's UserEventEnvelope (Stage 2) and payment-service's PlatformEventEnvelope (Stage 8). */
public record OrderEventEnvelope(
        String eventId,
        String eventType,
        int schemaVersion,
        String occurredAt,
        Map<String, Object> payload
) {
}
