package com.vicinia.orderservice.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

/**
 * order-service's own copy of an inventory-service client — not a shared
 * library with cart-service's, since each service only needs the small
 * slice of inventory-service's contract relevant to its own job (matching
 * every other cross-service client in this project: no shared client
 * modules, each service owns its own view of what it calls).
 */
@Component
public class InventoryClient {

    private final RestTemplate restTemplate;

    public InventoryClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** Returns false on 409 (insufficient stock) — inventory-service has already rolled back any partial reservation from this same call internally (Stage 5). Any other error propagates. */
    public boolean reserve(UUID orderId, List<ReserveItem> items) {
        try {
            restTemplate.postForEntity(
                    "http://INVENTORY-SERVICE/api/inventory/reservations/reserve",
                    new ReserveRequest(orderId, items), Void.class);
            return true;
        } catch (HttpClientErrorException.Conflict e) {
            return false;
        }
    }

    public void confirm(UUID orderId) {
        restTemplate.postForEntity(
                "http://INVENTORY-SERVICE/api/inventory/reservations/confirm", new OrderIdBody(orderId), Void.class);
    }

    public void release(UUID orderId) {
        restTemplate.postForEntity(
                "http://INVENTORY-SERVICE/api/inventory/reservations/release", new OrderIdBody(orderId), Void.class);
    }

    public record ReserveItem(UUID listingId, int quantity) {
    }

    public record ReserveRequest(UUID orderId, List<ReserveItem> items) {
    }

    public record OrderIdBody(UUID orderId) {
    }
}
