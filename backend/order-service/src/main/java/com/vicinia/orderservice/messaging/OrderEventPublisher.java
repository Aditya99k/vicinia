package com.vicinia.orderservice.messaging;

import com.vicinia.common.event.EventEnvelope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Publishes to order-events, partitioned by orderId. Two events have real,
 * testable consumers today: order.cancelled (payment-service refunds a
 * paid order — ARCHITECTURE.md §7) and order.confirmed (cart-service clears
 * the cart — §7's own dependency table names this explicitly, and Stage 6
 * deliberately deferred building it since order-service, the only possible
 * producer, didn't exist yet; it does now). order.created has no consumer
 * yet — expected, same as every event shipped ahead of its first consumer
 * throughout this project. Every payload carries userId, not just orderId
 * — a consumer reacting asynchronously needs enough in the event itself to
 * act, not a callback to order-service to find out who the order belonged to.
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

    public void publishConfirmed(UUID orderId, UUID userId) {
        kafkaTemplate.send(TOPIC, orderId.toString(), EventEnvelope.of("order.confirmed", new OrderUserPayload(orderId.toString(), userId.toString())));
    }

    public void publishCancelled(UUID orderId, UUID userId, BigDecimal totalAmount) {
        var payload = new OrderCancelledPayload(orderId.toString(), userId.toString(), totalAmount.toString());
        kafkaTemplate.send(TOPIC, orderId.toString(), EventEnvelope.of("order.cancelled", payload));
    }

    public record OrderUserPayload(String orderId, String userId) {
    }

    public record OrderCancelledPayload(String orderId, String userId, String totalAmount) {
    }
}
