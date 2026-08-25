package com.vicinia.orderservice.client;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
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

    /**
     * Returns false on 409 (insufficient stock) — inventory-service has
     * already rolled back any partial reservation from this same call
     * internally (Stage 5). Any other error propagates.
     *
     * <p>@Retryable (Stage 15, ARCHITECTURE.md §12) only on genuinely
     * transient failures — a connection issue (ResourceAccessException) or
     * a 5xx from inventory-service itself — never on the 409 handled above,
     * which is a real, correct business answer, not a blip. Safe to retry
     * because reservation is idempotent on (orderId, productId) per §11: a
     * retry after a response that actually succeeded server-side is a
     * no-op, not a double-reservation.
     */
    @Retryable(retryFor = {ResourceAccessException.class, HttpServerErrorException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2.0))
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
