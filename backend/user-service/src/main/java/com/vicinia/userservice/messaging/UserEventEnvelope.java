package com.vicinia.userservice.messaging;

import java.util.Map;

/**
 * user-service's own local view of the wire shape auth-service publishes
 * (ARCHITECTURE.md §9) — not a shared Java type. Two services deserializing
 * a generic EventEnvelope<T> via Jackson's embedded type headers would mean
 * user-service loading auth-service's own package classes off Kafka, which
 * defeats the point of them being separate deployable services. Instead the
 * producer sends type-header-free JSON and this consumer parses the
 * envelope generically, reading known fields out of `payload` by hand for
 * whichever eventType it recognizes.
 */
public record UserEventEnvelope(
        String eventId,
        String eventType,
        int schemaVersion,
        String occurredAt,
        Map<String, Object> payload
) {
}
