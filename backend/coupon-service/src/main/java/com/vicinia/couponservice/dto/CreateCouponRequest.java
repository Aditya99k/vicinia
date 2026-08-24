package com.vicinia.couponservice.dto;

import com.vicinia.couponservice.domain.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateCouponRequest(
        @NotBlank String code,
        String description,
        @NotNull DiscountType discountType,
        @NotNull @DecimalMin("0.01") BigDecimal discountValue,
        @DecimalMin("0.01") BigDecimal maxDiscountAmount,
        @DecimalMin("0.0") BigDecimal minOrderValue,
        @Min(1) Integer usageLimit,
        @Min(1) Integer perUserLimit,
        Instant validFrom,
        Instant validUntil
) {
}
