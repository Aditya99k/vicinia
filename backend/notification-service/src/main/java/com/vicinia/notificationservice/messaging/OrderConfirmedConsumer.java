package com.vicinia.notificationservice.messaging;

import com.vicinia.notificationservice.service.NotificationService;
import com.vicinia.notificationservice.util.IdFormat;
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
 *
 * <p>Notifies both sides of the same event: the customer ("your order is
 * confirmed") and the merchant ("you have a new order") — found and fixed
 * as a real gap (the merchantId was already on the payload but never
 * read). The two calls need distinct eventIds — record()'s idempotency
 * is a plain existsByEventId check, so reusing the envelope's own eventId
 * for both would make the second call a silent no-op, not a second
 * notification.
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
        String merchantId = (String) envelope.payload().get("merchantId");
        String totalAmount = (String) envelope.payload().get("totalAmount");
        String shortId = IdFormat.shorten(orderId);
        String amountText = totalAmount != null ? " (₹" + totalAmount + ")" : "";

        notificationService.record(envelope.eventId(), envelope.eventType(), userId,
                "Order confirmed",
                "Your order #" + shortId + amountText + " has been confirmed and is being prepared.",
                orderId);

        if (merchantId != null) {
            notificationService.record(envelope.eventId() + ":merchant", envelope.eventType(), merchantId,
                    "New order received",
                    "You have a new order #" + shortId + amountText + " to prepare.",
                    orderId);
        }
    }

    @DltHandler
    public void onDlt(IncomingEventEnvelope envelope) {
        log.error("order-events message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
    }
}
