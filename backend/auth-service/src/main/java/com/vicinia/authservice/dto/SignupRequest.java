package com.vicinia.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "password must be at least 8 characters") String password,
        /** One of CUSTOMER, MERCHANT, DELIVERY_PARTNER. Defaults to CUSTOMER. ADMIN cannot self-register. */
        String role
) {
}
