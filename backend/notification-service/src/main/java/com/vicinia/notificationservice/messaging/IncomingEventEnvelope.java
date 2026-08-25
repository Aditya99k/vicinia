package com.vicinia.notificationservice.messaging;

import java.util.Map;

/** Generic envelope shared across all 4 consumers, on 4 different source topics — this service never needs any other service's own envelope type. */
public record IncomingEventEnvelope(String eventId, String eventType, int schemaVersion, String occurredAt, Map<String, Object> payload) {
}
