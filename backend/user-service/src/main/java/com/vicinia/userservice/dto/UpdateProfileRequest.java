package com.vicinia.userservice.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 120) String fullName,
        @Size(max = 20) String phone
) {
}
