package com.vicinia.cartservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateItemRequest(
        @NotNull @Min(1) Integer quantity
) {
}
