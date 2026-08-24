package com.vicinia.merchantservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ApplyRequest(
        @NotBlank String storeName,
        String description,
        @NotBlank String addressLine1,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String pincode,
        @NotEmpty @Valid List<DocumentDto> documents
) {
}
