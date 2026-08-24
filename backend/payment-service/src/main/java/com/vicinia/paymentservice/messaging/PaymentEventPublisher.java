package com.vicinia.paymentservice.messaging;

import com.vicinia.common.event.EventEnvelope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/** Publishes to payment-events, partitioned by orderId. No consumer exists yet (notification-service, Stage 12) — expected at this point, same as every event shipped ahead of its first consumer in this project. */
@Component
public class PaymentEventPublisher {

    private static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishSuccess(UUID orderId, UUID userId, BigDecimal amount) {
        var envelope = EventEnvelope.of("payment.success", new PaymentPayload(orderId.toString(), userId.toString(), amount.toString()));
        kafkaTemplate.send(TOPIC, orderId.toString(), envelope);
    }

    public void publishFailed(UUID orderId, UUID userId, BigDecimal amount) {
        var envelope = EventEnvelope.of("payment.failed", new PaymentPayload(orderId.toString(), userId.toString(), amount.toString()));
        kafkaTemplate.send(TOPIC, orderId.toString(), envelope);
    }

    public record PaymentPayload(String orderId, String userId, String amount) {
    }
}
