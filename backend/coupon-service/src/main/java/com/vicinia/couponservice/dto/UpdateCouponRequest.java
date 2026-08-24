package com.vicinia.couponservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.Instant;

/** Every field optional — a partial update, same convention as inventory-service's UpdateListingRequest. */
public record UpdateCouponRequest(
        String description,
        @DecimalMin("0.01") BigDecimal discountValue,
        @DecimalMin("0.01") BigDecimal maxDiscountAmount,
        @DecimalMin("0.0") BigDecimal minOrderValue,
        @Min(1) Integer usageLimit,
        @Min(1) Integer perUserLimit,
        Instant validFrom,
        Instant validUntil,
        Boolean active
) {
}
