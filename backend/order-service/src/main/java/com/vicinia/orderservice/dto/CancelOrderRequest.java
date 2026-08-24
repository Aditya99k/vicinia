package com.vicinia.orderservice.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelOrderRequest(
        @NotBlank String reason
) {
}
