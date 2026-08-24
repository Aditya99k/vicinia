package com.vicinia.orderservice.client;

import com.vicinia.common.security.HeaderNames;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Direct service-to-service call to cart-service — order-service manually
 * sets X-User-Id to the same value it itself received from the gateway,
 * propagating the already-verified identity to an internal call.
 * InternalRequestFilter doesn't care who set this header, only whether
 * X-Internal-Secret is present and correct (ADR 0008) — the interceptor
 * on the shared RestTemplate bean adds that automatically.
 */
@Component
public class CartClient {

    private final RestTemplate restTemplate;

    public CartClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public CartView getCart(UUID userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HeaderNames.USER_ID, userId.toString());
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange("http://CART-SERVICE/api/cart", HttpMethod.GET, entity, CartView.class).getBody();
    }

    public record CartView(UUID userId, UUID merchantId, List<CartLine> items, BigDecimal subtotal) {
    }

    public record CartLine(UUID listingId, String productId, String productName, UUID merchantId,
                            BigDecimal price, int quantity, int availableStock, boolean available, BigDecimal lineTotal) {
    }
}
