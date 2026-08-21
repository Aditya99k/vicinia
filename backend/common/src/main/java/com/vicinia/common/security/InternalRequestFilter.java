package com.vicinia.common.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Enforces gateway-only access (ADR 0008): rejects any request that doesn't
 * carry the shared internal secret api-gateway stamps on every proxied call.
 * Hitting a service's own port directly, bypassing the gateway, fails here.
 *
 * Registered manually as a @Bean in each service (see SecurityBeansConfig)
 * rather than via component scanning, so `common` never needs to be added
 * to a service's @ComponentScan base packages.
 */
public class InternalRequestFilter implements Filter {

    private static final List<String> EXEMPT_PREFIXES = List.of("/actuator");

    private final String expectedSecret;

    public InternalRequestFilter(String expectedSecret) {
        this.expectedSecret = expectedSecret;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();
        boolean exempt = EXEMPT_PREFIXES.stream().anyMatch(path::startsWith);
        if (exempt) {
            chain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader(HeaderNames.INTERNAL_SECRET);
        if (provided == null || !provided.equals(expectedSecret)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Direct access is not permitted — go through api-gateway");
            return;
        }

        chain.doFilter(request, response);
    }
}
