package com.vicinia.orderservice.messaging;

import com.vicinia.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ARCHITECTURE.md §7/§9: order-service is delivery-events' one consumer
 * group — "delivery status changes over minutes/hours; order-service just
 * mirrors it" (§8). No other service consumes this topic today, so there's
 * no collision risk yet, but the explicit suffix is applied anyway for the
 * same reason as every listener since Stage 10: it costs nothing now and
 * avoids the exact cross-consumer-group DLT collision the moment a second
 * consumer (e.g. notification-service, Stage 12) joins this topic.
 */
@Component
public class DeliveryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventConsumer.class);

    private final OrderService orderService;

    public DeliveryEventConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 2.0), autoCreateTopics = "true",
            retryTopicSuffix = "-order-service-retry", dltTopicSuffix = "-order-service-dlt")
    @KafkaListener(topics = "delivery-events", groupId = "order-service")
    public void onDeliveryEvent(PaymentEventEnvelope envelope) {
        UUID orderId = UUID.fromString((String) envelope.payload().get("orderId"));
        switch (envelope.eventType()) {
            case "delivery.assigned" -> orderService.assignedToDelivery(orderId);
            case "delivery.delivered" -> orderService.delivered(orderId);
            default -> log.debug("Ignoring unrecognized eventType '{}'", envelope.eventType());
        }
    }

    @DltHandler
    public void onDlt(PaymentEventEnvelope envelope) {
        log.error("delivery-events message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
    }
}
