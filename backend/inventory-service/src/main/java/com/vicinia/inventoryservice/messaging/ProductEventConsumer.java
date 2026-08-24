package com.vicinia.inventoryservice.messaging;

import com.vicinia.inventoryservice.client.CatalogClient;
import com.vicinia.inventoryservice.domain.KnownProduct;
import com.vicinia.inventoryservice.repository.KnownProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
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
 *
 * attempts=6/delay=3000 (fixed, no multiplier) is deliberately generous —
 * this is a migration from Stage 5's own custom DefaultErrorHandler bean
 * (removed — KafkaConsumerConfig.java), which existed for exactly this
 * reason: the earliest-offset backfill fires a REST call the instant this
 * service starts, before the @LoadBalanced RestTemplate's Eureka view of
 * CATALOG-SERVICE has populated, and a too-fast default retry exhausts
 * before that cache warms up. @RetryableTopic is the architecturally
 * intended mechanism (ARCHITECTURE.md §9 — non-blocking retry topics, not
 * a container-level blocking backoff), so this preserves the same ~15-18s
 * of headroom that fix already proved sufficient, just via the right tool.
 * Explicit, service-qualified retry/DLT suffixes rather than Spring
 * Kafka's default (source-topic-based) naming — no other service consumes
 * product-events today, so there's no collision yet, but naming it
 * explicitly now costs nothing and avoids the exact cross-consumer-group
 * DLT collision found and fixed on order-events/user-events in this same
 * stage the moment a second consumer of this topic is ever added.
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

    @RetryableTopic(attempts = "6", backoff = @Backoff(delay = 3000), autoCreateTopics = "true", retryTopicSuffix = "-inventory-service-retry", dltTopicSuffix = "-inventory-service-dlt")
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

    @DltHandler
    public void onDlt(ProductEnvelope envelope) {
        log.error("product-events message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
    }

    public record ProductEnvelope(String eventId, String eventType, int schemaVersion, Instant occurredAt,
                                   ProductCreatedPayload payload) {
    }

    public record ProductCreatedPayload(String productId) {
    }
}
