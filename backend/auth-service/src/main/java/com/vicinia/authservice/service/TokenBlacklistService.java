package com.vicinia.authservice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Access-token blacklist (ARCHITECTURE.md §13 — key format blacklist:{token},
 * here keyed by the token's jti). api-gateway checks this on every request
 * for a route requiring auth; the TTL is capped at the token's own natural
 * expiry, so a blacklist entry never outlives the token it blocks.
 */
@Service
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "blacklist:";

    private final StringRedisTemplate redis;

    public TokenBlacklistService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void blacklist(String jti, Instant expiresAt) {
        long ttlSeconds = Math.max(1, Duration.between(Instant.now(), expiresAt).getSeconds());
        redis.opsForValue().set(KEY_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + jti));
    }
}
