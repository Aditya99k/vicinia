package com.vicinia.orderservice.client;

import com.vicinia.common.security.HeaderNames;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
public class CouponClient {

    private final RestTemplate restTemplate;

    public CouponClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** Empty on any rejection (unknown code, inactive, min-order not met, usage limit hit) — coupon-service's specific reason is logged upstream if needed, order-service just needs yes/no. */
    public Optional<BigDecimal> apply(UUID userId, String code, UUID orderId, BigDecimal orderValue) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HeaderNames.USER_ID, userId.toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ApplyRequest> entity = new HttpEntity<>(new ApplyRequest(code, orderId, orderValue), headers);
        try {
            ApplyResponse response = restTemplate.postForObject(
                    "http://COUPON-SERVICE/api/coupons/apply", entity, ApplyResponse.class);
            return Optional.ofNullable(response).map(ApplyResponse::discountAmount);
        } catch (HttpClientErrorException e) {
            return Optional.empty();
        }
    }

    public record ApplyRequest(String code, UUID orderId, BigDecimal orderValue) {
    }

    public record ApplyResponse(UUID couponId, UUID orderId, BigDecimal discountAmount, String usedAt) {
    }
}
