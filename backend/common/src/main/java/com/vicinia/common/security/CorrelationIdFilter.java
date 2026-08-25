package com.vicinia.common.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * Stage 16 (ARCHITECTURE.md §15): reads the correlation ID api-gateway
 * stamped on every proxied request, puts it in MDC for structured logging
 * (see each service's logging.pattern.level override, config-repo/
 * application.yml), and echoes it back on the response so a client can
 * quote it in a support request. Falls back to generating one if absent —
 * a direct hit bypassing the gateway is already rejected by
 * InternalRequestFilter, but this filter runs first (order 0, vs. 1) so
 * even that rejected request's own log line carries a real ID.
 *
 * <p>Registered manually as a @Bean in each service (see
 * SecurityBeansConfig), same reasoning as InternalRequestFilter: no
 * component scanning of `common`.
 */
public class CorrelationIdFilter implements Filter {

    public static final String MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String correlationId = request.getHeader(HeaderNames.CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        try {
            MDC.put(MDC_KEY, correlationId);
            response.setHeader(HeaderNames.CORRELATION_ID, correlationId);
            chain.doFilter(request, response);
        } finally {
            // Servlet threads are pooled and reused — leaving this set would
            // leak one request's ID into the next request served by the
            // same thread.
            MDC.remove(MDC_KEY);
        }
    }
}
