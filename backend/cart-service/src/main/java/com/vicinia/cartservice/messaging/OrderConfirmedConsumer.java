package com.vicinia.cartservice.messaging;

import com.vicinia.cartservice.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Closes the gap Stage 6 explicitly deferred: ARCHITECTURE.md §7 lists
 * order.confirmed -> clear cart as cart-service's one inbound event
 * dependency, but order-service (the only possible producer) didn't exist
 * yet. It does now (Stage 8). Idempotent by construction — deleting an
 * already-empty/nonexistent Redis key is a harmless no-op, so replaying
 * the same event twice (Kafka's at-least-once delivery) is safe without
 * needing a separate processed-events check.
 */
@Component
public class OrderConfirmedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmedConsumer.class);

    private final CartRepository cartRepository;

    public OrderConfirmedConsumer(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @KafkaListener(topics = "order-events", groupId = "cart-service")
    public void onOrderEvent(OrderEventEnvelope envelope) {
        if (!"order.confirmed".equals(envelope.eventType())) {
            return;
        }
        UUID userId = UUID.fromString((String) envelope.payload().get("userId"));
        cartRepository.delete(userId);
        log.info("Cleared cart for {} on order.confirmed", userId);
    }
}
