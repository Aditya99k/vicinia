package com.vicinia.merchantservice.messaging;

import java.util.Map;

/** merchant-service's own local view of order-service's wire shape (ARCHITECTURE.md §9) — not a shared Java type, same reasoning as every other cross-service consumer envelope in this project. */
public record OrderEventEnvelope(
        String eventId,
        String eventType,
        int schemaVersion,
        String occurredAt,
        Map<String, Object> payload
) {
}
