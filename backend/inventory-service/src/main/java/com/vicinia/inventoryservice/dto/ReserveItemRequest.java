package com.vicinia.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReserveItemRequest(
        @NotNull UUID listingId,
        @NotNull @Min(1) Integer quantity
) {
}
