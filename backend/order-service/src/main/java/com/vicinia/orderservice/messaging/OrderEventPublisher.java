package com.vicinia.orderservice.messaging;

import com.vicinia.common.event.EventEnvelope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Publishes to order-events, partitioned by orderId. Three events have
 * real, testable consumers today: order.cancelled (payment-service refunds
 * a paid order — ARCHITECTURE.md §7), order.confirmed (cart-service clears
 * the cart — §7's own dependency table names this explicitly, and Stage 6
 * deliberately deferred building it since order-service, the only possible
 * producer, didn't exist yet), and now order.confirmed also feeds
 * merchant-service's minimal order-acceptance flow (Stage 11 — pulled
 * forward, see BUILD_TRACKER.md's notes on why). order.created has no
 * consumer yet — expected, same as every event shipped ahead of its first
 * consumer throughout this project.
 *
 * order.confirmed carries merchantId, not just orderId/userId, because
 * merchant-service's new consumer needs to know which of its own merchants
 * this order belongs to without a callback to order-service to ask — same
 * "enough in the event to act" reasoning as adding userId here in Stage 8.
 */
@Component
public class OrderEventPublisher {

    private static final String TOPIC = "order-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishCreated(UUID orderId, UUID userId) {
        kafkaTemplate.send(TOPIC, orderId.toString(), EventEnvelope.of("order.created", new OrderUserPayload(orderId.toString(), userId.toString())));
    }

    public void publishConfirmed(UUID orderId, UUID userId, UUID merchantId) {
        var payload = new OrderConfirmedPayload(orderId.toString(), userId.toString(), merchantId.toString());
        kafkaTemplate.send(TOPIC, orderId.toString(), EventEnvelope.of("order.confirmed", payload));
    }

    public void publishCancelled(UUID orderId, UUID userId, BigDecimal totalAmount) {
        var payload = new OrderCancelledPayload(orderId.toString(), userId.toString(), totalAmount.toString());
        kafkaTemplate.send(TOPIC, orderId.toString(), EventEnvelope.of("order.cancelled", payload));
    }

    public record OrderUserPayload(String orderId, String userId) {
    }

    public record OrderConfirmedPayload(String orderId, String userId, String merchantId) {
    }

    public record OrderCancelledPayload(String orderId, String userId, String totalAmount) {
    }
}
