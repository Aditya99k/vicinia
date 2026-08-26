package com.vicinia.orderservice.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves "who is delivering this order, and how do I reach them" for the
 * customer's own order-detail page — a two-hop lookup (delivery-service for
 * the assignment, user-service for the actual contact info), both
 * gracefully absent (empty Optional) rather than an error, since most
 * orders simply don't have a rider assigned yet.
 */
@Component
public class RiderClient {

    private final RestTemplate restTemplate;

    public RiderClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<UUID> partnerUserIdFor(UUID orderId) {
        try {
            var response = restTemplate.getForObject(
                    "http://DELIVERY-SERVICE/api/delivery/tasks/{orderId}/rider", Map.class, orderId);
            return Optional.ofNullable(response)
                    .map(r -> (String) r.get("partnerUserId"))
                    .map(UUID::fromString);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public Optional<ContactSummary> contactSummary(UUID userId) {
        try {
            return Optional.ofNullable(
                    restTemplate.getForObject("http://USER-SERVICE/api/users/{userId}/contact-summary", ContactSummary.class, userId));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public record ContactSummary(String fullName, String phone) {
    }
}
