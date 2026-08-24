package com.vicinia.orderservice.messaging;

import com.vicinia.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
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
 */
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final OrderService orderService;

    public PaymentEventConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

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
}
