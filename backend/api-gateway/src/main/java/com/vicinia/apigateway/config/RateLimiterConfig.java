package com.vicinia.apigateway.config;

import com.vicinia.common.security.HeaderNames;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Per-user rate limiting (Stage 15, ARCHITECTURE.md §12 — "abuse/scraping
 * protection, Redis token bucket"). Spring Cloud Gateway's built-in
 * RequestRateLimiter filter already implements a token bucket against
 * Redis (RedisRateLimiter, auto-configured once spring-boot-starter-data-
 * redis is on the classpath — already true here since Stage 1) — the only
 * piece actually missing was this KeyResolver, telling the filter what to
 * key the bucket on.
 *
 * <p>Keys by the authenticated caller's X-User-Id when present. Runs after
 * AuthGlobalFilter (HIGHEST_PRECEDENCE, so it always runs first) has
 * already validated the JWT and stamped that header on the mutated
 * request the rest of the chain — including this resolver — actually
 * sees; a client can't forge it since AuthGlobalFilter strips any
 * client-supplied copy first. Public/unauthenticated paths (signup,
 * login, catalog search) have no X-User-Id yet, so those fall back to the
 * caller's IP — still meaningfully rate-limits credential-stuffing or
 * scraping attempts against the public endpoints specifically.
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst(HeaderNames.USER_ID);
            if (userId != null) {
                return Mono.just(userId);
            }
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ip);
        };
    }
}
