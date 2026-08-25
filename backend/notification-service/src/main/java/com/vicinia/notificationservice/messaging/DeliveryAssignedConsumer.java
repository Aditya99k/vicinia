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
 * delivery-events also carries delivery.delivered — this consumer only
 * cares about delivery.assigned, the one signal a partner actually needs
 * ("you have a pickup"). Was published by delivery-service's
 * DeliveryEventPublisher.publishAssigned from the start but had no
 * consumer anywhere — a genuine gap (not in ARCHITECTURE.md §7's original
 * table, added once notified partners turned out to matter): a partner
 * had no server-side signal a task existed at all.
 */
@Component
public class DeliveryAssignedConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryAssignedConsumer.class);

    private final NotificationService notificationService;

    public DeliveryAssignedConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 2.0), autoCreateTopics = "true",
            retryTopicSuffix = "-notification-service-delivery-retry", dltTopicSuffix = "-notification-service-delivery-dlt")
    @KafkaListener(topics = "delivery-events", groupId = "notification-service")
    public void onDeliveryEvent(IncomingEventEnvelope envelope) {
        if (!"delivery.assigned".equals(envelope.eventType())) {
            return;
        }
        String orderId = (String) envelope.payload().get("orderId");
        String partnerId = (String) envelope.payload().get("partnerId");
        notificationService.record(envelope.eventId(), envelope.eventType(), partnerId,
                "New pickup assigned",
                "You've been assigned order " + orderId + " for pickup.");
    }

    @DltHandler
    public void onDlt(IncomingEventEnvelope envelope) {
        log.error("delivery-events message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
    }
}
