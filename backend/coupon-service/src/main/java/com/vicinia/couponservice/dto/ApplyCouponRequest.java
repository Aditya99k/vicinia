package com.vicinia.couponservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplyCouponRequest(
        @NotBlank String code,
        @NotNull UUID orderId,
        @NotNull @DecimalMin("0.0") BigDecimal orderValue
) {
}
