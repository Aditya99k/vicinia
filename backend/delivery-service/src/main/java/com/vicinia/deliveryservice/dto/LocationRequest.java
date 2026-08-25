package com.vicinia.deliveryservice.dto;

import jakarta.validation.constraints.NotNull;

/** Shared shape for go-online (needs a starting location) and a live ping. */
public record LocationRequest(
        @NotNull Double latitude,
        @NotNull Double longitude
) {
}
