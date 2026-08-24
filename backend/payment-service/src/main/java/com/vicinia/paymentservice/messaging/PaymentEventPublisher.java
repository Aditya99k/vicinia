package com.vicinia.paymentservice.messaging;

import com.vicinia.common.event.EventEnvelope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Publishes to payment-events, partitioned by orderId. Stage 9 gave this
 * its first real consumer: order-service's PaymentEventConsumer, for the
 * Razorpay path specifically (ARCHITECTURE.md §8 — Razorpay confirmation
 * is async by nature, unlike wallet's synchronous REST response). method
 * lets that consumer tell the two payment paths apart and ignore
 * wallet-originated events it already handled synchronously in the same
 * HTTP request that triggered them.
 */
@Component
public class PaymentEventPublisher {

    private static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishSuccess(UUID orderId, UUID userId, BigDecimal amount, String method) {
        var envelope = EventEnvelope.of("payment.success", new PaymentPayload(orderId.toString(), userId.toString(), amount.toString(), method));
        kafkaTemplate.send(TOPIC, orderId.toString(), envelope);
    }

    public void publishFailed(UUID orderId, UUID userId, BigDecimal amount, String method) {
        var envelope = EventEnvelope.of("payment.failed", new PaymentPayload(orderId.toString(), userId.toString(), amount.toString(), method));
        kafkaTemplate.send(TOPIC, orderId.toString(), envelope);
    }

    public record PaymentPayload(String orderId, String userId, String amount, String method) {
    }
}
