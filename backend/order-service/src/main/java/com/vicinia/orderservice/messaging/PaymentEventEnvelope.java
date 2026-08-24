package com.vicinia.orderservice.messaging;

import java.util.Map;

/** order-service's own local view of payment-service's wire shape (ARCHITECTURE.md §9) — not a shared Java type, same reasoning as every other cross-service consumer envelope in this project. */
public record PaymentEventEnvelope(
        String eventId,
        String eventType,
        int schemaVersion,
        String occurredAt,
        Map<String, Object> payload
) {
}
