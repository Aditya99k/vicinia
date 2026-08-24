package com.vicinia.orderservice.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentClient {

    private final RestTemplate restTemplate;

    public PaymentClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** Returns false on 402 (insufficient balance) — an expected, handled outcome. Anything else propagates as a real error. */
    public boolean payWithWallet(UUID userId, UUID orderId, BigDecimal amount) {
        try {
            restTemplate.postForEntity(
                    "http://PAYMENT-SERVICE/api/payments/wallet/pay", new PayRequest(userId, orderId, amount), Void.class);
            return true;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) {
                return false;
            }
            throw e;
        }
    }

    public void refund(UUID userId, UUID orderId, BigDecimal amount) {
        restTemplate.postForEntity(
                "http://PAYMENT-SERVICE/api/payments/wallet/refund", new RefundRequest(userId, orderId, amount), Void.class);
    }

    public record PayRequest(UUID userId, UUID orderId, BigDecimal amount) {
    }

    public record RefundRequest(UUID userId, UUID orderId, BigDecimal amount) {
    }
}
