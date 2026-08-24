package com.vicinia.orderservice.messaging;

import com.vicinia.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The Razorpay path's async resolution — ARCHITECTURE.md §8: "Razorpay
 * confirmation is async by nature (webhook, arbitrary delay)". Filters out
 * WALLET-originated events on purpose: wallet's own synchronous flow in
 * OrderService.placeOrder already confirms/fails the order in the same
 * HTTP request that triggered the payment, so reacting to its event here
 * too would be redundant — not wrong (the idempotent PAYMENT_PENDING guard
 * in OrderService would still make it a safe no-op), but the method filter
 * makes the intent explicit rather than relying on that guard alone.
 *
 * @RetryableTopic (Stage 10): confirmFromPaymentEvent/failFromPaymentEvent
 * call out to inventory-service — a transient failure there retries
 * before landing on a DLT. Explicit, service-qualified retry/DLT suffixes
 * rather than Spring Kafka's default (source-topic-based) naming — no
 * other service consumes payment-events today, so there's no collision
 * yet, but naming it explicitly now costs nothing and avoids the exact
 * cross-consumer-group DLT collision found and fixed on order-events/
 * user-events in this same stage the moment a second consumer of this
 * topic is ever added (e.g. notification-service, Stage 12).
 */
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final OrderService orderService;

    public PaymentEventConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 2.0), autoCreateTopics = "true",
            retryTopicSuffix = "-order-service-retry", dltTopicSuffix = "-order-service-dlt")
    @KafkaListener(topics = "payment-events", groupId = "order-service")
    public void onPaymentEvent(PaymentEventEnvelope envelope) {
        String method = (String) envelope.payload().get("method");
        if (!"RAZORPAY".equals(method)) {
            return;
        }

        UUID orderId = UUID.fromString((String) envelope.payload().get("orderId"));
        switch (envelope.eventType()) {
            case "payment.success" -> orderService.confirmFromPaymentEvent(orderId);
            case "payment.failed" -> orderService.failFromPaymentEvent(orderId);
            default -> log.debug("Ignoring unrecognized eventType '{}'", envelope.eventType());
        }
    }

    @DltHandler
    public void onDlt(PaymentEventEnvelope envelope) {
        log.error("payment-events message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
    }
}
