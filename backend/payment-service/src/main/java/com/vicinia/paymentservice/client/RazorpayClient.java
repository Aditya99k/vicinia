package com.vicinia.paymentservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Calls Razorpay's real REST API directly (https://api.razorpay.com) — a
 * plain RestTemplate, not the shared @LoadBalanced one every other client
 * in this project uses, since Razorpay is a genuine third party, not an
 * Eureka-registered internal service. Auth is HTTP Basic with the key
 * id/secret pair (Razorpay's own documented auth scheme), not
 * X-Internal-Secret — this call never touches our own internal trust
 * boundary at all.
 */
@Component
public class RazorpayClient {

    private static final String ORDERS_URL = "https://api.razorpay.com/v1/orders";

    private final RestTemplate restTemplate = new RestTemplate();
    private final String keyId;
    private final String keySecret;

    public RazorpayClient(@Value("${vicinia.razorpay.key-id}") String keyId,
                           @Value("${vicinia.razorpay.key-secret}") String keySecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    /** amount is in rupees; Razorpay's API wants paise (integer, smallest currency unit) — see ARCHITECTURE's money-handling convention. */
    public String createOrder(BigDecimal amount, String receipt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(keyId, keySecret);

        long amountInPaise = amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
        Map<String, Object> body = Map.of(
                "amount", amountInPaise,
                "currency", "INR",
                "receipt", receipt
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        Map<?, ?> response = restTemplate.postForObject(ORDERS_URL, entity, Map.class);
        return (String) response.get("id");
    }

    public String getKeyId() {
        return keyId;
    }
}
