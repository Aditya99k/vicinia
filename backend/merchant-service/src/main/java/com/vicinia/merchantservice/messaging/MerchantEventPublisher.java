package com.vicinia.merchantservice.messaging;

import com.vicinia.common.event.EventEnvelope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes to merchant-events (ARCHITECTURE.md §9 — one topic per
 * aggregate, partitioned by merchantId). No consumers exist yet
 * (catalog-service and notification-service, per §7's dependency table,
 * haven't been built) — that's expected at this stage, the same way
 * user.registered had no consumer for the length of Stage 2 until
 * user-service existed.
 */
@Component
public class MerchantEventPublisher {

    private static final String TOPIC = "merchant-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MerchantEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishApproved(String merchantId) {
        var envelope = EventEnvelope.of("merchant.approved", new MerchantStatusPayload(merchantId, "APPROVED"));
        kafkaTemplate.send(TOPIC, merchantId, envelope);
    }

    public void publishSuspended(String merchantId, String reason) {
        var envelope = EventEnvelope.of("merchant.suspended", new MerchantSuspendedPayload(merchantId, reason));
        kafkaTemplate.send(TOPIC, merchantId, envelope);
    }

    public record MerchantStatusPayload(String merchantId, String status) {
    }

    public record MerchantSuspendedPayload(String merchantId, String reason) {
    }
}
