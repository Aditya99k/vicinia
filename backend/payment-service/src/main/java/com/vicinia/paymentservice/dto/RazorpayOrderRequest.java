package com.vicinia.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/** Internal-only, called by order-service — same shape as PayRequest. */
public record RazorpayOrderRequest(
        @NotNull UUID userId,
        @NotNull UUID orderId,
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {
}
