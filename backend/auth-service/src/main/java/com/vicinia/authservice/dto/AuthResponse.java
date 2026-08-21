package com.vicinia.authservice.dto;

import java.util.Set;

public record AuthResponse(
        String userId,
        String email,
        Set<String> roles,
        Set<String> permissions,
        String accessToken,
        String refreshToken,
        long expiresInSeconds
) {
}
