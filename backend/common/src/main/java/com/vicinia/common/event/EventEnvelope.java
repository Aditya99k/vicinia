package com.vicinia.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Every Kafka event published in Vicinia is wrapped in this envelope
 * (ARCHITECTURE.md §9): a UUID eventId for consumer-side idempotency, an
 * eventType discriminator (topics are one-per-aggregate, not one-per-event),
 * and a schemaVersion so the payload shape can evolve without breaking old
 * consumers mid-deploy.
 */
public record EventEnvelope<T>(
        String eventId,
        String eventType,
        int schemaVersion,
        Instant occurredAt,
        T payload
) {
    public static <T> EventEnvelope<T> of(String eventType, T payload) {
        return new EventEnvelope<>(UUID.randomUUID().toString(), eventType, 1, Instant.now(), payload);
    }
}
