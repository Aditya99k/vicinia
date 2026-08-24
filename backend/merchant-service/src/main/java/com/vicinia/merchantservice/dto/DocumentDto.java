package com.vicinia.merchantservice.dto;

import jakarta.validation.constraints.NotBlank;

public record DocumentDto(
        @NotBlank String documentType,
        @NotBlank String referenceUrl
) {
}
