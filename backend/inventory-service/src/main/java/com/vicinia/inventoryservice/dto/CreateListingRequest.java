package com.vicinia.inventoryservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateListingRequest(
        @NotBlank String productId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @NotNull @Min(0) Integer availableStock
) {
}
