package com.vicinia.userservice.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        @NotBlank String label,
        @NotBlank String line1,
        String line2,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String pincode,
        boolean isDefault
) {
}
