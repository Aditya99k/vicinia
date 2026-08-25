package com.vicinia.apigateway.security;

import com.vicinia.common.security.HeaderNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Stage 16 (ARCHITECTURE.md §15): generates a correlation ID at the
 * gateway if the client didn't already send one, stamps it on the
 * forwarded request (every domain service's CorrelationIdFilter reads it
 * from there and puts it in MDC), and echoes it on the response so a
 * client can quote it in a support request.
 *
 * <p>Runs first, ahead of AuthGlobalFilter (see that class's getOrder()),
 * so every request — even one that ends up 401/403 — gets a real ID.
 *
 * <p>Deliberately not threaded through MDC here: api-gateway is WebFlux,
 * where a single request's processing hops across multiple event-loop
 * threads, so traditional ThreadLocal-based MDC doesn't follow it the way
 * it does on every other (Servlet, thread-per-request) service in this
 * project. Correlating this service's own log lines to one request would
 * need Reactor Context propagation wired through Micrometer's tracing
 * bridge — real, but a separate piece of work from "propagate the ID to
 * every downstream service," which is what this filter actually does.
 * The one log line below carries the ID explicitly instead.
 */
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdGlobalFilter.class);

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(HeaderNames.CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        String finalCorrelationId = correlationId;

        log.info("correlationId={} method={} path={}", finalCorrelationId, request.getMethod(), request.getURI().getPath());

        exchange.getResponse().getHeaders().set(HeaderNames.CORRELATION_ID, finalCorrelationId);

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(HeaderNames.CORRELATION_ID, finalCorrelationId)
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
}
