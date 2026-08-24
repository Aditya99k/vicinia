package com.vicinia.cartservice.messaging;

import com.vicinia.cartservice.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
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
 *
 * @RetryableTopic (Stage 10): a malformed userId (not a valid UUID) throws
 * before ever reaching Redis — retries 3 times with a 1s delay, then lands
 * on a DLT. Short backoff deliberately — nothing here depends on an
 * external cache warming up the way inventory-service's product-events
 * consumer does. Explicit, service-qualified retry/DLT suffixes, not
 * Spring Kafka's default naming — payment-service's PlatformEventConsumer
 * also applies @RetryableTopic to this same order-events topic, and the
 * default (source-topic-based, not consumer-group-based) naming would
 * collide both consumer groups onto one shared DLT — confirmed by actually
 * triggering a failure here and seeing payment-service's own @DltHandler
 * fire on it too, before this fix. See BUILD_TRACKER.md's Stage 10 notes.
 */
@Component
public class OrderConfirmedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmedConsumer.class);

    private final CartRepository cartRepository;

    public OrderConfirmedConsumer(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000), autoCreateTopics = "true",
            retryTopicSuffix = "-cart-service-retry", dltTopicSuffix = "-cart-service-dlt")
    @KafkaListener(topics = "order-events", groupId = "cart-service")
    public void onOrderEvent(OrderEventEnvelope envelope) {
        if (!"order.confirmed".equals(envelope.eventType())) {
            return;
        }
        UUID userId = UUID.fromString((String) envelope.payload().get("userId"));
        cartRepository.delete(userId);
        log.info("Cleared cart for {} on order.confirmed", userId);
    }

    @DltHandler
    public void onDlt(OrderEventEnvelope envelope) {
        log.error("order-events message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
    }
}
