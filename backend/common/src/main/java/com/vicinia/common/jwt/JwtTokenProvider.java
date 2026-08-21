package com.vicinia.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Issues and verifies the short-lived JWT access token (ARCHITECTURE.md
 * §14). Refresh tokens are deliberately NOT JWTs — they're opaque,
 * Redis-backed, rotatable-on-use (see auth-service's TokenService) — only
 * the access token is a signed, self-contained JWT.
 *
 * One instance of this class, built from the same vicinia.jwt.secret, must
 * be shared by auth-service (issues tokens) and api-gateway (verifies them)
 * — that's why it lives in `common` instead of being duplicated.
 */
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final Duration accessTokenTtl;

    public JwtTokenProvider(String secret, long accessTokenTtlMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = Duration.ofMinutes(accessTokenTtlMinutes);
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }

    public String issueAccessToken(String userId, String email, List<String> roles, List<String> permissions) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("email", email)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(signingKey)
                .compact();
    }

    /** @throws JwtException if the token is malformed, expired, or has a bad signature. */
    @SuppressWarnings("unchecked")
    public TokenClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new TokenClaims(
                claims.getId(),
                claims.getSubject(),
                claims.get("email", String.class),
                (List<String>) claims.get("roles", List.class),
                (List<String>) claims.get("permissions", List.class),
                claims.getExpiration().toInstant()
        );
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
