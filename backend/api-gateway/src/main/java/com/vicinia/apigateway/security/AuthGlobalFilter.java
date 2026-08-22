package com.vicinia.apigateway.security;

import com.vicinia.apigateway.config.PublicPathsProperties;
import com.vicinia.common.jwt.JwtTokenProvider;
import com.vicinia.common.jwt.TokenClaims;
import com.vicinia.common.security.HeaderNames;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Runs on every proxied request (ARCHITECTURE.md §14). Two jobs:
 *
 * <p>1. Always: strip any client-supplied X-User-* / X-Internal-Secret
 * headers and re-stamp X-Internal-Secret with the real value — a caller
 * cannot forge identity by just setting these headers themselves (IDOR
 * prevention).
 *
 * <p>2. For any path not in vicinia.gateway.public-paths: require a valid,
 * non-blacklisted Bearer JWT, then inject X-User-Id/Email/Roles/Permissions
 * from its claims — this is the only place in the system a JWT is parsed
 * on the request path; every downstream service just trusts the headers.
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtTokenProvider jwtTokenProvider;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final PublicPathsProperties gatewayProperties;
    private final String internalSecret;

    public AuthGlobalFilter(JwtTokenProvider jwtTokenProvider,
                             ReactiveStringRedisTemplate redisTemplate,
                             PublicPathsProperties gatewayProperties,
                             org.springframework.core.env.Environment env) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
        this.gatewayProperties = gatewayProperties;
        this.internalSecret = env.getRequiredProperty("vicinia.internal-secret");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // CORS preflight carries no Authorization header by design — Spring's
        // own CORS handling (spring.cloud.gateway.globalcors) answers these
        // before routing, but skip auth here too as defense-in-depth so a
        // framework-ordering surprise never turns into a broken preflight.
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String path = request.getURI().getPath();

        ServerHttpRequest.Builder mutated = request.mutate()
                .headers(headers -> {
                    headers.remove(HeaderNames.USER_ID);
                    headers.remove(HeaderNames.USER_EMAIL);
                    headers.remove(HeaderNames.USER_ROLES);
                    headers.remove(HeaderNames.USER_PERMISSIONS);
                    headers.set(HeaderNames.INTERNAL_SECRET, internalSecret);
                });

        if (isPublic(path)) {
            return chain.filter(exchange.mutate().request(mutated.build()).build());
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing bearer token");
        }
        String token = authHeader.substring(7);

        TokenClaims claims;
        try {
            claims = jwtTokenProvider.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(exchange, "Invalid or expired token");
        }

        return redisTemplate.hasKey("blacklist:" + claims.jti())
                .defaultIfEmpty(false)
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        return unauthorized(exchange, "Token has been revoked");
                    }
                    ServerHttpRequest authedRequest = mutated
                            .header(HeaderNames.USER_ID, claims.userId())
                            .header(HeaderNames.USER_EMAIL, claims.email())
                            .header(HeaderNames.USER_ROLES, String.join(",", claims.roles()))
                            .header(HeaderNames.USER_PERMISSIONS, String.join(",", claims.permissions()))
                            .build();
                    return chain.filter(exchange.mutate().request(authedRequest).build());
                });
    }

    private boolean isPublic(String path) {
        List<String> publicPaths = gatewayProperties.getPublicPaths();
        return publicPaths.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        String json = "{\"status\":401,\"error\":\"" + message + "\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
