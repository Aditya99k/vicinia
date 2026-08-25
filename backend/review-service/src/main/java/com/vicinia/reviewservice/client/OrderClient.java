package com.vicinia.reviewservice.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** review-service's eligibility check (Stage 13): has this user received a delivered order for this product. */
@Component
public class OrderClient {

    private final RestTemplate restTemplate;

    public OrderClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean hasDeliveredProduct(String userId, String productId) {
        String url = UriComponentsBuilder.fromUriString("http://ORDER-SERVICE/api/orders/internal/delivered")
                .queryParam("userId", userId)
                .queryParam("productId", productId)
                .toUriString();
        HasDeliveredResponse response = restTemplate.getForObject(url, HasDeliveredResponse.class);
        return response != null && response.delivered();
    }

    public record HasDeliveredResponse(boolean delivered) {
    }
}
