package com.vicinia.common.observability;

import com.vicinia.common.security.CorrelationIdFilter;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Runs immediately before each record is handed to its @KafkaListener
 * method (Spring Kafka's own per-record hook, not the raw Kafka
 * ConsumerInterceptor SPI — that one only sees whole batches, before
 * Spring's container dispatches individual records, which would make MDC
 * timing wrong for a batch of more than one). Reads the header
 * CorrelationIdProducerInterceptor wrote on the producing side; falls
 * back to a fresh ID for messages that predate this stage or came from
 * outside this system.
 */
public class CorrelationIdRecordInterceptor implements RecordInterceptor<Object, Object> {

    @Override
    public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        Header header = record.headers().lastHeader(CorrelationIdProducerInterceptor.HEADER);
        String correlationId = header != null
                ? new String(header.value(), StandardCharsets.UTF_8)
                : UUID.randomUUID().toString();
        MDC.put(CorrelationIdFilter.MDC_KEY, correlationId);
        return record;
    }

    @Override
    public void afterRecord(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        MDC.remove(CorrelationIdFilter.MDC_KEY);
    }
}
