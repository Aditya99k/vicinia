package com.vicinia.deliveryservice.messaging;

import com.vicinia.deliveryservice.service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * A fifth consumer group on order-events (alongside cart-service,
 * payment-service, merchant-service, order-service itself) — the explicit,
 * service-qualified retry/DLT suffix is what keeps all five isolated
 * (Stage 10's collision fix). merchantLatitude/Longitude come straight
 * from the event payload — merchant-service put them there specifically so
 * this consumer never needs a callback to ask where to search.
 */
@Component
public class OrderReadyConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderReadyConsumer.class);

    private final DeliveryService deliveryService;

    public OrderReadyConsumer(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 2.0), autoCreateTopics = "true",
            retryTopicSuffix = "-delivery-service-order-retry", dltTopicSuffix = "-delivery-service-order-dlt")
    @KafkaListener(topics = "order-events", groupId = "delivery-service")
    public void onOrderEvent(IncomingEventEnvelope envelope) {
        if (!"order.ready".equals(envelope.eventType())) {
            return;
        }
        UUID orderId = UUID.fromString((String) envelope.payload().get("orderId"));
        UUID merchantId = UUID.fromString((String) envelope.payload().get("merchantId"));
        Double latitude = Double.valueOf((String) envelope.payload().get("latitude"));
        Double longitude = Double.valueOf((String) envelope.payload().get("longitude"));
        deliveryService.createAndAssign(orderId, merchantId, latitude, longitude);
    }

    @DltHandler
    public void onDlt(IncomingEventEnvelope envelope) {
        log.error("order-events message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
    }
}
