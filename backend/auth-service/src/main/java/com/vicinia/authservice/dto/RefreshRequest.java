package com.vicinia.authservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * The client presents both userId and the opaque refresh token, matching
 * the Redis key format documented in ARCHITECTURE.md §13 (refresh:{userId})
 * exactly — no separate token-to-user reverse index needed.
 */
public record RefreshRequest(
        @NotBlank String userId,
        @NotBlank String refreshToken
) {
}
