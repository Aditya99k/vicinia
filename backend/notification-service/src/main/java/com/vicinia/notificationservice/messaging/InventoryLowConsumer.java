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
 * inventory-events also carries inventory.out — ARCHITECTURE.md §7's
 * table lists only inventory.low for notification-service, so inventory.out
 * is deliberately ignored here. The recipient is the merchant's own
 * ownerUserId (inventory-service's "merchantId" field), the same
 * merchantId-means-ownerUserId convention used system-wide (see Stage 11's
 * bug fix in MerchantOrderService for what happens when a service forgets
 * this).
 */
@Component
public class InventoryLowConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryLowConsumer.class);

    private final NotificationService notificationService;

    public InventoryLowConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 2.0), autoCreateTopics = "true",
            retryTopicSuffix = "-notification-service-inventory-retry", dltTopicSuffix = "-notification-service-inventory-dlt")
    @KafkaListener(topics = "inventory-events", groupId = "notification-service")
    public void onInventoryEvent(IncomingEventEnvelope envelope) {
        if (!"inventory.low".equals(envelope.eventType())) {
            return;
        }
        String productId = (String) envelope.payload().get("productId");
        String merchantId = (String) envelope.payload().get("merchantId");
        Object availableStock = envelope.payload().get("availableStock");
        notificationService.record(envelope.eventId(), envelope.eventType(), merchantId,
                "Low stock alert",
                "One of your listings is running low (" + availableStock + " left) — consider restocking.",
                productId);
    }

    @DltHandler
    public void onDlt(IncomingEventEnvelope envelope) {
        log.error("inventory-events message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
    }
}
