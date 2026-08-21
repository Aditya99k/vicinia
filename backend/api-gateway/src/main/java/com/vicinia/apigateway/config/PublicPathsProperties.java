package com.vicinia.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds vicinia.gateway.public-paths — the allow-list AuthGlobalFilter
 * skips JWT validation for.
 *
 * Named PublicPathsProperties, not GatewayProperties: Spring Cloud Gateway
 * itself already registers an internal bean literally named
 * "gatewayProperties" (org.springframework.cloud.gateway.config.
 * GatewayProperties, its route/predicate config) — reusing that simple
 * class name collides on the default bean name and fails startup.
 */
@Component
@ConfigurationProperties(prefix = "vicinia.gateway")
public class PublicPathsProperties {

    private List<String> publicPaths = new ArrayList<>();

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }
}
