package com.vicinia.inventoryservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderIdRequest(
        @NotNull UUID orderId
) {
}
