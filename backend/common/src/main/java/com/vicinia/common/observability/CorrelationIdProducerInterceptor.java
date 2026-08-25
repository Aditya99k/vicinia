package com.vicinia.common.observability;

import com.vicinia.common.security.CorrelationIdFilter;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Configured purely via {@code spring.kafka.producer.properties.
 * interceptor.classes} in each service's application.yml — no publisher
 * class needs to change. Copies the current request thread's correlation
 * ID (already in MDC via CorrelationIdFilter, since a Kafka publish
 * happens synchronously within the same request that triggered it) onto
 * the outgoing record as a real Kafka header (ARCHITECTURE.md §15 —
 * "into Kafka event headers", not a payload field, so every consumer of
 * every topic gets it for free regardless of that topic's own event
 * schema).
 */
public class CorrelationIdProducerInterceptor implements ProducerInterceptor<Object, Object> {

    public static final String HEADER = "correlationId";

    @Override
    public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            record.headers().add(new RecordHeader(HEADER, correlationId.getBytes(StandardCharsets.UTF_8)));
        }
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }
}
