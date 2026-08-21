package com.vicinia.authservice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Opaque, Redis-backed, rotate-on-use refresh tokens (ARCHITECTURE.md §13
 * — key format refresh:{userId}, 30-day TTL). Deliberately not a JWT: a
 * refresh token needs to be revocable server-side, which a self-contained
 * signed token can't be without a separate blacklist anyway.
 */
@Service
public class RefreshTokenService {

    private static final Duration TTL = Duration.ofDays(30);
    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redis;

    public RefreshTokenService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String issue(String userId) {
        String token = UUID.randomUUID().toString();
        redis.opsForValue().set(KEY_PREFIX + userId, token, TTL);
        return token;
    }

    /** Rotation: a valid presented token is consumed (deleted) — callers must issue a fresh one. */
    public boolean validateAndConsume(String userId, String presentedToken) {
        String stored = redis.opsForValue().get(KEY_PREFIX + userId);
        if (stored == null || !stored.equals(presentedToken)) {
            return false;
        }
        redis.delete(KEY_PREFIX + userId);
        return true;
    }

    public void revoke(String userId) {
        redis.delete(KEY_PREFIX + userId);
    }
}
