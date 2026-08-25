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
 * order-service's own consumer of order-events, alongside cart-service's
 * and merchant-service's — ADR 0004: merchant-service manages its own task
 * state but publishes merchant.accepted/merchant.rejected/order.ready,
 * which order-service consumes to advance its canonical status. Reuses
 * PaymentEventEnvelope (Stage 9) as a generic envelope type — structurally
 * identical regardless of source topic, same as every cross-service
 * consumer envelope in this project.
 */
@Component
public class OrderFulfillmentConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderFulfillmentConsumer.class);

    private final OrderService orderService;

    public OrderFulfillmentConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 2.0), autoCreateTopics = "true",
            retryTopicSuffix = "-order-service-fulfillment-retry", dltTopicSuffix = "-order-service-fulfillment-dlt")
    @KafkaListener(topics = "order-events", groupId = "order-service")
    public void onOrderEvent(PaymentEventEnvelope envelope) {
        UUID orderId = UUID.fromString((String) envelope.payload().get("orderId"));
        switch (envelope.eventType()) {
            case "merchant.accepted" -> orderService.acceptedByMerchant(orderId);
            case "merchant.rejected" -> orderService.rejectedByMerchant(orderId, (String) envelope.payload().get("reason"));
            case "order.ready" -> orderService.markReadyForPickup(orderId);
            default -> log.debug("Ignoring unrecognized eventType '{}'", envelope.eventType());
        }
    }

    @DltHandler
    public void onDlt(PaymentEventEnvelope envelope) {
        log.error("order-events message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
    }
}
