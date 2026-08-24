package com.vicinia.cartservice.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * The live price/availability check on every cart read (BUILD_TRACKER
 * Stage 6) — cart-service never stores price or stock itself, it always
 * asks inventory-service for the current truth. Same Eureka-resolved,
 * @LoadBalanced RestTemplate + X-Internal-Secret pattern as
 * inventory-service's own CatalogClient (Stage 5).
 */
@Component
public class InventoryClient {

    private final RestTemplate restTemplate;

    public InventoryClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<ListingRef> fetch(UUID listingId) {
        try {
            ListingRef listing = restTemplate.getForObject(
                    "http://INVENTORY-SERVICE/api/inventory/listings/{id}", ListingRef.class, listingId);
            return Optional.ofNullable(listing);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    public record ListingRef(UUID id, UUID merchantId, String productId, String productName,
                              String productCategory, BigDecimal price, int availableStock,
                              int reservedStock, boolean active) {
    }
}
