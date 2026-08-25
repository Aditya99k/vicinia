package com.vicinia.orderservice.config;

import com.vicinia.common.security.HeaderNames;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Same pattern as inventory-service's and cart-service's RestClientConfig —
 * every internal call carries X-Internal-Secret automatically; X-User-Id is
 * added per-call where a downstream endpoint needs it (cart, coupon).
 *
 * <p>Timeouts (Stage 15, ARCHITECTURE.md §12 — "Timeout on every Feign
 * client," this project's actual RestTemplate-based equivalent): without
 * one, a hung downstream service ties up the calling thread indefinitely,
 * eventually exhausting the whole request-handling pool. 2s to connect is
 * generous for same-cluster internal calls; 5s to read covers a real but
 * slow response without masking a genuinely stuck one.
 */
@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(@Value("${vicinia.internal-secret}") String internalSecret) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add(HeaderNames.INTERNAL_SECRET, internalSecret);
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}
