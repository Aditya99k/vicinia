package com.vicinia.inventoryservice.messaging;

import com.vicinia.inventoryservice.client.CatalogClient;
import com.vicinia.inventoryservice.domain.KnownProduct;
import com.vicinia.inventoryservice.repository.KnownProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * The project's first Kafka consumer. catalog-service's product.created
 * payload is deliberately thin — just a productId, no name/category — so
 * this does one REST fetch per product to populate the local KnownProduct
 * cache, rather than every future listing-creation needing its own call to
 * catalog-service. group-id "inventory-service" with auto-offset-reset:
 * earliest (see application.yml) means this also backfills every product
 * approved before this service ever existed, on first boot.
 */
@Component
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);

    private final KnownProductRepository knownProductRepository;
    private final CatalogClient catalogClient;

    public ProductEventConsumer(KnownProductRepository knownProductRepository, CatalogClient catalogClient) {
        this.knownProductRepository = knownProductRepository;
        this.catalogClient = catalogClient;
    }

    @KafkaListener(topics = "product-events", groupId = "inventory-service")
    public void onProductEvent(ProductEnvelope envelope) {
        if (!"product.created".equals(envelope.eventType())) {
            return;
        }
        String productId = envelope.payload().productId();
        catalogClient.fetch(productId).ifPresentOrElse(
                product -> knownProductRepository.save(new KnownProduct(product.id(), product.name(), product.category())),
                () -> log.warn("product.created received for {} but catalog-service has no such product", productId)
        );
    }

    public record ProductEnvelope(String eventId, String eventType, int schemaVersion, Instant occurredAt,
                                   ProductCreatedPayload payload) {
    }

    public record ProductCreatedPayload(String productId) {
    }
}
