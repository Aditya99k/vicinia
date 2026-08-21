package com.vicinia.authservice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Short-lived (15 min) reset tokens, Redis-backed like OTP/refresh tokens
 * (ARCHITECTURE.md §13's key-format style, extended with reset:{token}).
 */
@Service
public class PasswordResetService {

    private static final Duration TTL = Duration.ofMinutes(15);
    private static final String KEY_PREFIX = "reset:";

    private final StringRedisTemplate redis;

    public PasswordResetService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String createToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        redis.opsForValue().set(KEY_PREFIX + token, userId.toString(), TTL);
        return token;
    }

    public Optional<UUID> consumeToken(String token) {
        String userId = redis.opsForValue().get(KEY_PREFIX + token);
        if (userId == null) {
            return Optional.empty();
        }
        redis.delete(KEY_PREFIX + token);
        return Optional.of(UUID.fromString(userId));
    }
}
