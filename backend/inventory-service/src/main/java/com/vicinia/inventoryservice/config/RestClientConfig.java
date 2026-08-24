package com.vicinia.inventoryservice.config;

import com.vicinia.common.security.HeaderNames;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * A RestTemplate for calling other domain services directly (Eureka
 * service-id, not a literal host:port), bypassing api-gateway. This is the
 * first synchronous service-to-service call in the project (CatalogClient ->
 * catalog-service) rather than gateway -> service, so it has to add the
 * X-Internal-Secret itself — InternalRequestFilter on the receiving side
 * doesn't care whether the gateway or another service added it, only that
 * it's present and correct (ADR 0008).
 */
@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(@Value("${vicinia.internal-secret}") String internalSecret) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add(HeaderNames.INTERNAL_SECRET, internalSecret);
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}
