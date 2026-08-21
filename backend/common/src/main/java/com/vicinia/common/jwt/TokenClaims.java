package com.vicinia.common.jwt;

import java.time.Instant;
import java.util.List;

/** Parsed, verified contents of an access token — see JwtTokenProvider. */
public record TokenClaims(
        String jti,
        String userId,
        String email,
        List<String> roles,
        List<String> permissions,
        Instant expiresAt
) {
}
