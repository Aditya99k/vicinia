package com.vicinia.paymentservice.messaging;

import java.util.Map;

/**
 * payment-service's own local view of the wire shape other services
 * publish (ARCHITECTURE.md §9) — not a shared Java type, matching
 * user-service's UserEventConsumer pattern from Stage 2: two services
 * deserializing a generic EventEnvelope<T> via Jackson type headers would
 * mean payment-service loading auth-service's and order-service's own
 * package classes off Kafka, defeating the point of separate deployable
 * services. This consumes BOTH user-events and order-events, each with
 * different eventTypes and payload shapes, so payload stays a generic Map
 * parsed by hand per recognized eventType — see PlatformEventConsumer.
 */
public record PlatformEventEnvelope(
        String eventId,
        String eventType,
        int schemaVersion,
        String occurredAt,
        Map<String, Object> payload
) {
}
