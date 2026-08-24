package com.vicinia.merchantservice.dto;

import jakarta.validation.constraints.NotBlank;

/** Used for both reject and suspend — same shape, different endpoint. */
public record ReasonRequest(
        @NotBlank String reason
) {
}
