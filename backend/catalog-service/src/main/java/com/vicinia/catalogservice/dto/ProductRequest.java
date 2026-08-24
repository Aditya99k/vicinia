package com.vicinia.catalogservice.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

/** Shared shape for both admin direct-create and merchant request-to-create — only what happens to the resulting status differs. */
public record ProductRequest(
        @NotBlank String name,
        String brand,
        @NotBlank String category,
        String description,
        List<String> images,
        Map<String, Object> attributes
) {
}
