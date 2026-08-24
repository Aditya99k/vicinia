package com.vicinia.cartservice.repository;

import com.vicinia.cartservice.domain.Cart;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.UUID;

/** ARCHITECTURE.md §12 — key pattern cart:{userId}, TTL reset (rolling) on every write, not on a plain read. */
@Repository
public class CartRepository {

    private final RedisTemplate<String, Cart> redisTemplate;
    private final Duration ttl;

    public CartRepository(RedisTemplate<String, Cart> redisTemplate,
                           @Value("${vicinia.cart.ttl-days:7}") long ttlDays) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofDays(ttlDays);
    }

    public Cart findOrCreate(UUID userId) {
        Cart cart = redisTemplate.opsForValue().get(key(userId));
        return cart != null ? cart : new Cart(userId);
    }

    public void save(Cart cart) {
        redisTemplate.opsForValue().set(key(cart.getUserId()), cart, ttl);
    }

    public void delete(UUID userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(UUID userId) {
        return "cart:" + userId;
    }
}
