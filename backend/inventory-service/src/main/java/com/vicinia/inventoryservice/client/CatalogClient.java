package com.vicinia.inventoryservice.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Validates a productId against catalog-service (ARCHITECTURE.md §7 — the
 * one synchronous dependency this service has) via Eureka-resolved
 * service-id, not a literal host:port. Used both as the fallback when
 * InventoryService.resolveProduct misses the local KnownProduct cache, and
 * by ProductEventConsumer to fetch full product details right after
 * receiving a thin product.created event.
 */
@Component
public class CatalogClient {

    private final RestTemplate restTemplate;

    public CatalogClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<ProductRef> fetch(String productId) {
        try {
            ProductRef product = restTemplate.getForObject(
                    "http://CATALOG-SERVICE/api/catalog/products/{id}", ProductRef.class, productId);
            return Optional.ofNullable(product);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    /** Deliberately only the fields inventory-service actually needs — Jackson ignores the rest of catalog-service's ProductResponse. */
    public record ProductRef(String id, String name, String category) {
    }
}
