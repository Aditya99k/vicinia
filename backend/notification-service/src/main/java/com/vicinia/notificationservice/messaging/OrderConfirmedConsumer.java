package com.vicinia.notificationservice.messaging;

import com.vicinia.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * order-events also carries order.created, merchant.accepted,
 * merchant.rejected, and order.ready (Stage 11) — this consumer, like
 * ARCHITECTURE.md §7's table, only cares about order.confirmed.
 */
@Component
public class OrderConfirmedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmedConsumer.class);

    private final NotificationService notificationService;

    public OrderConfirmedConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 2.0), autoCreateTopics = "true",
            retryTopicSuffix = "-notification-service-order-retry", dltTopicSuffix = "-notification-service-order-dlt")
    @KafkaListener(topics = "order-events", groupId = "notification-service")
    public void onOrderEvent(IncomingEventEnvelope envelope) {
        if (!"order.confirmed".equals(envelope.eventType())) {
            return;
        }
        String orderId = (String) envelope.payload().get("orderId");
        String userId = (String) envelope.payload().get("userId");
        notificationService.record(envelope.eventId(), envelope.eventType(), userId,
                "Order confirmed",
                "Your order " + orderId + " has been confirmed and is being prepared.");
    }

    @DltHandler
    public void onDlt(IncomingEventEnvelope envelope) {
        log.error("order-events message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
    }
}
