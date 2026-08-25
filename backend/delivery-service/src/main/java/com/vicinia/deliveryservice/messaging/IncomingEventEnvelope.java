package com.vicinia.deliveryservice.messaging;

import java.util.Map;

/** delivery-service's own local view of other services' wire shapes (ARCHITECTURE.md §9) — shared across both consumers (user-events, order-events) since the envelope shape is structurally identical regardless of source topic, same reasoning as every cross-service consumer envelope in this project. */
public record IncomingEventEnvelope(
        String eventId,
        String eventType,
        int schemaVersion,
        String occurredAt,
        Map<String, Object> payload
) {
}
