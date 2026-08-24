package com.vicinia.inventoryservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

/** Every field optional — a partial update, only supplied fields change. */
public record UpdateListingRequest(
        @DecimalMin(value = "0.01") BigDecimal price,
        @Min(0) Integer availableStock,
        Boolean active
) {
}
