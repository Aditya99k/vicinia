package com.vicinia.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/** Internal-only (X-Internal-Secret), called by order-service — no gateway-injected X-User-Id, so the caller states who's paying. */
public record PayRequest(
        @NotNull UUID userId,
        @NotNull UUID orderId,
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {
}
