package com.vicinia.merchantservice.messaging;

import com.vicinia.merchantservice.service.MerchantOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * merchant-service's second Kafka consumer, and its first on delivery-events
 * — closes the loop Stage 18's queue-persistence fix opened: a READY task
 * now stays visible (so whoever's at the counter can check a rider's
 * pickup against the order id) right up until the order is actually
 * delivered, not just until the merchant clicked "ready".
 *
 * <p>Deliberately typed as OrderEventEnvelope, not a separate
 * DeliveryEventEnvelope — application.yml pins one fixed
 * spring.json.value.default.type for this whole service
 * (use.type.headers: false), so every @KafkaListener here, regardless of
 * topic, gets deserialized into that same class no matter its declared
 * parameter type. The two shapes are structurally identical anyway
 * ({@code eventId, eventType, schemaVersion, occurredAt, payload}); a
 * differently-named twin class here would just silently never be the type
 * actually handed to the method (this exact bug, caught live: every
 * message landed straight on the DLT with a MessageConversionException
 * before this fix).
 */
@Component
public class DeliveryDeliveredConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryDeliveredConsumer.class);

    private final MerchantOrderService merchantOrderService;

    public DeliveryDeliveredConsumer(MerchantOrderService merchantOrderService) {
        this.merchantOrderService = merchantOrderService;
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 2.0), autoCreateTopics = "true",
            retryTopicSuffix = "-merchant-service-delivery-retry", dltTopicSuffix = "-merchant-service-delivery-dlt")
    @KafkaListener(topics = "delivery-events", groupId = "merchant-service")
    public void onDeliveryEvent(OrderEventEnvelope envelope) {
        if (!"delivery.delivered".equals(envelope.eventType())) {
            return;
        }
        UUID orderId = UUID.fromString((String) envelope.payload().get("orderId"));
        merchantOrderService.completeFromDelivery(orderId);
    }

    @DltHandler
    public void onDlt(OrderEventEnvelope envelope) {
        log.error("delivery-events message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
    }
}
