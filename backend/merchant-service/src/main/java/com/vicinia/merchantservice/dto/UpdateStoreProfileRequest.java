package com.vicinia.merchantservice.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateStoreProfileRequest(
        @NotBlank String storeName,
        String description,
        @NotBlank String addressLine1,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String pincode,
        Double latitude,
        Double longitude,
        Double deliveryRadiusKm
) {
}
