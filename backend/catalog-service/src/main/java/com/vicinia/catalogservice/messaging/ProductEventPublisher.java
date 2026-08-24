package com.vicinia.catalogservice.messaging;

import com.vicinia.common.event.EventEnvelope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes to product-events (ARCHITECTURE.md §9), partitioned by
 * productId. inventory-service (Stage 5) will consume product.created to
 * validate a productId exists before a merchant can create a listing
 * against it.
 */
@Component
public class ProductEventPublisher {

    private static final String TOPIC = "product-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ProductEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishCreated(String productId) {
        var envelope = EventEnvelope.of("product.created", new ProductCreatedPayload(productId));
        kafkaTemplate.send(TOPIC, productId, envelope);
    }

    public record ProductCreatedPayload(String productId) {
    }
}
