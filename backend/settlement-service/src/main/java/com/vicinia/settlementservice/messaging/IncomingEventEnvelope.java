package com.vicinia.settlementservice.messaging;

import java.util.Map;

public record IncomingEventEnvelope(String eventId, String eventType, int schemaVersion, String occurredAt, Map<String, Object> payload) {
}
