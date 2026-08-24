package com.vicinia.orderservice.config;

import com.vicinia.common.security.HeaderNames;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** Same pattern as inventory-service's and cart-service's RestClientConfig — every internal call carries X-Internal-Secret automatically; X-User-Id is added per-call where a downstream endpoint needs it (cart, coupon). */
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
