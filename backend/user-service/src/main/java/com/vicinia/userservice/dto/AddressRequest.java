package com.vicinia.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddressRequest(
        @NotBlank String label,
        @NotBlank String line1,
        String line2,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String pincode,
        boolean isDefault,
        // Mandatory as of Stage 18's geolocation work — real merchant
        // discovery (MerchantService.nearby) and delivery-partner
        // navigation both depend on an address actually having
        // coordinates, not just a typed city string (which, in practice,
        // meant "Bangalore" vs "Bengaluru" made merchants invisible to
        // customers whose address used the other spelling). Enforced here
        // too, not just in AddressFormModal — the frontend blocking submit
        // without a location isn't the real guarantee, this is.
        @NotNull Double latitude,
        @NotNull Double longitude
) {
}
