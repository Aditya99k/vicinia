package com.vicinia.settlementservice.messaging;

import com.vicinia.common.event.EventEnvelope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Publishes to settlement-events (ADR 0009 — a new aggregate this service
 * itself owns, distinct from order-events/payment-events). No consumer
 * exists yet, same as every event shipped ahead of its first consumer
 * throughout this project (order.ready in Stage 11, inventory.low before
 * Stage 12). settlement.completed fires when a Payout reaches PAID — the
 * point at which the merchant has actually (simulated-ly) been paid, not
 * when the SettlementEntry was first created.
 */
@Component
public class SettlementEventPublisher {

    private static final String TOPIC = "settlement-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SettlementEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishCompleted(UUID payoutId, UUID merchantId, BigDecimal totalAmount) {
        var payload = new SettlementCompletedPayload(payoutId.toString(), merchantId.toString(), totalAmount.toString());
        kafkaTemplate.send(TOPIC, merchantId.toString(), EventEnvelope.of("settlement.completed", payload));
    }

    public record SettlementCompletedPayload(String payoutId, String merchantId, String totalAmount) {
    }
}
