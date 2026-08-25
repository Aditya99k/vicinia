package com.vicinia.paymentservice.messaging;

import com.vicinia.common.event.EventEnvelope;
import io.micrometer.core.instrument.MeterRegistry;
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
 *
 * <p>Also the single instrumentation point for Stage 16's payment
 * success/failure-by-method dashboard — both publishSuccess/publishFailed
 * are already called from all 4 real success/failure sites (wallet and
 * Razorpay), so counting here covers both payment paths without touching
 * WalletService or RazorpayPaymentService at all.
 */
@Component
public class PaymentEventPublisher {

    private static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    public PaymentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
    }

    public void publishSuccess(UUID orderId, UUID userId, BigDecimal amount, String method) {
        var envelope = EventEnvelope.of("payment.success", new PaymentPayload(orderId.toString(), userId.toString(), amount.toString(), method));
        kafkaTemplate.send(TOPIC, orderId.toString(), envelope);
        meterRegistry.counter("payment.outcome", "method", method, "outcome", "success").increment();
    }

    public void publishFailed(UUID orderId, UUID userId, BigDecimal amount, String method) {
        var envelope = EventEnvelope.of("payment.failed", new PaymentPayload(orderId.toString(), userId.toString(), amount.toString(), method));
        kafkaTemplate.send(TOPIC, orderId.toString(), envelope);
        meterRegistry.counter("payment.outcome", "method", method, "outcome", "failure").increment();
    }

    public record PaymentPayload(String orderId, String userId, String amount, String method) {
    }
}
