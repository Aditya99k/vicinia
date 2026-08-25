package com.vicinia.merchantservice.messaging;

import com.vicinia.common.event.EventEnvelope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes to order-events (not merchant-events) — despite the
 * merchant.* event names, these describe a change to the ORDER aggregate
 * (ADR 0009: one topic per aggregate, not per producing service), and
 * order-service is the consumer that actually needs them. ADR 0004 lists
 * merchant.accepted and order.ready explicitly as events order-service
 * consumes to advance its own canonical status; merchant.rejected is the
 * natural counterpart (CONFIRMED -> MERCHANT_REJECTED already exists in
 * order-service's own transition guard from Stage 8).
 *
 * Payloads carry only what merchant-service actually knows — orderId (and
 * a reason, or the store's own lat/lng for order.ready so delivery-service
 * can search for a nearby partner without a callback). userId/totalAmount
 * for the refund-on-rejection path live in order-service's own DB already;
 * no need to duplicate them here.
 */
@Component
public class MerchantOrderEventPublisher {

    private static final String TOPIC = "order-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MerchantOrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAccepted(UUID orderId) {
        kafkaTemplate.send(TOPIC, orderId.toString(), EventEnvelope.of("merchant.accepted", new OrderIdPayload(orderId.toString())));
    }

    public void publishRejected(UUID orderId, String reason) {
        var payload = new RejectedPayload(orderId.toString(), reason);
        kafkaTemplate.send(TOPIC, orderId.toString(), EventEnvelope.of("merchant.rejected", payload));
    }

    public void publishReady(UUID orderId, UUID merchantId, Double latitude, Double longitude) {
        var payload = new ReadyPayload(orderId.toString(), merchantId.toString(),
                String.valueOf(latitude), String.valueOf(longitude));
        kafkaTemplate.send(TOPIC, orderId.toString(), EventEnvelope.of("order.ready", payload));
    }

    public record OrderIdPayload(String orderId) {
    }

    public record RejectedPayload(String orderId, String reason) {
    }

    public record ReadyPayload(String orderId, String merchantId, String latitude, String longitude) {
    }
}
