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
 * payment-events also carries payment.success — ARCHITECTURE.md §7's
 * table lists only payment.failed for notification-service (a successful
 * payment is already implied by the order.confirmed notification above),
 * so payment.success is deliberately ignored here.
 */
@Component
public class PaymentFailedConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentFailedConsumer.class);

    private final NotificationService notificationService;

    public PaymentFailedConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 2.0), autoCreateTopics = "true",
            retryTopicSuffix = "-notification-service-payment-retry", dltTopicSuffix = "-notification-service-payment-dlt")
    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    public void onPaymentEvent(IncomingEventEnvelope envelope) {
        if (!"payment.failed".equals(envelope.eventType())) {
            return;
        }
        String orderId = (String) envelope.payload().get("orderId");
        String userId = (String) envelope.payload().get("userId");
        String amount = (String) envelope.payload().get("amount");
        notificationService.record(envelope.eventId(), envelope.eventType(), userId,
                "Payment failed",
                "Payment of ₹" + amount + " for order #" + IdFormat.shorten(orderId) + " could not be completed.",
                orderId);
    }

    @DltHandler
    public void onDlt(IncomingEventEnvelope envelope) {
        log.error("payment-events message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
    }
}
