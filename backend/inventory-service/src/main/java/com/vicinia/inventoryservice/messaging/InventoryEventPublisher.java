package com.vicinia.inventoryservice.messaging;

import com.vicinia.common.event.EventEnvelope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes to inventory-events (ARCHITECTURE.md §9), partitioned by
 * productId. Fire-and-forget (§8 — "low-stock alert" has no synchronous
 * caller waiting on it); no consumer exists yet (notification-service,
 * Stage 12) — expected at this point, same as every other event that's
 * shipped ahead of its first consumer in this project.
 */
@Component
public class InventoryEventPublisher {

    private static final String TOPIC = "inventory-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishLow(String productId, UUID merchantId, UUID listingId, int availableStock) {
        var envelope = EventEnvelope.of("inventory.low",
                new StockPayload(productId, merchantId.toString(), listingId.toString(), availableStock));
        kafkaTemplate.send(TOPIC, productId, envelope);
    }

    public void publishOut(String productId, UUID merchantId, UUID listingId) {
        var envelope = EventEnvelope.of("inventory.out",
                new StockPayload(productId, merchantId.toString(), listingId.toString(), 0));
        kafkaTemplate.send(TOPIC, productId, envelope);
    }

    public record StockPayload(String productId, String merchantId, String listingId, int availableStock) {
    }
}
