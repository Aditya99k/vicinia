package com.vicinia.deliveryservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Redis GEO half of ADR-style "Postgres + Redis GEO" split (§4.4's own
 * atomic-conditional-UPDATE reasoning for inventory doesn't apply here —
 * this isn't a correctness-under-concurrency problem, just "where do I
 * cheaply store and query live coordinates without a DB write per ping").
 * One sorted set, GEOADD-backed, holds every currently-online partner;
 * going offline removes the member (GEO commands are built on ZSET, so a
 * plain ZREM is the correct way to remove one — Spring Data Redis's
 * GeoOperations has no direct "remove").
 */
@Component
public class PartnerGeoService {

    private static final String KEY = "delivery:partners:online";

    private final StringRedisTemplate redisTemplate;
    private final double searchRadiusKm;

    public PartnerGeoService(StringRedisTemplate redisTemplate,
                              @Value("${vicinia.delivery.search-radius-km:15}") double searchRadiusKm) {
        this.redisTemplate = redisTemplate;
        this.searchRadiusKm = searchRadiusKm;
    }

    public void addOrUpdate(UUID partnerId, double latitude, double longitude) {
        redisTemplate.opsForGeo().add(KEY, new Point(longitude, latitude), partnerId.toString());
    }

    public void remove(UUID partnerId) {
        redisTemplate.opsForZSet().remove(KEY, partnerId.toString());
    }

    /** Nearest online partners within the configured radius, closest first, excluding anyone already offered and declined this task. */
    public List<UUID> findNearest(double latitude, double longitude, Set<UUID> exclude, int count) {
        Circle searchArea = new Circle(new Point(longitude, latitude), new Distance(searchRadiusKm, Metrics.KILOMETERS));
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .sortAscending()
                .limit(count + exclude.size());

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().radius(KEY, searchArea, args);
        if (results == null) {
            return List.of();
        }

        return results.getContent().stream()
                .map(result -> UUID.fromString(result.getContent().getName()))
                .filter(id -> !exclude.contains(id))
                .limit(count)
                .toList();
    }
}
