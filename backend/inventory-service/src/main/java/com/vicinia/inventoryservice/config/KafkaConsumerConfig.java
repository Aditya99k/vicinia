package com.vicinia.inventoryservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * ProductEventConsumer's earliest-offset backfill runs a REST call
 * (CatalogClient -> catalog-service) the moment this service starts — but
 * the @LoadBalanced RestTemplate's client-side view of CATALOG-SERVICE
 * takes a few seconds to populate after a cold start (the same load-balancer
 * -cache-lag class of timing issue already seen elsewhere in this project,
 * e.g. api-gateway's early 503s in Stage 4). Spring Kafka's default error
 * handler retries near-instantly and exhausts its attempts well before that
 * cache catches up, silently skipping the record. A 3s backoff over 8
 * attempts (~24s of headroom) is comfortably past what's been observed
 * empirically for the load balancer to learn about a freshly-registered
 * service — Spring Boot auto-wires any single CommonErrorHandler bean into
 * the autoconfigured listener container factory.
 */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(3000L, 8));
    }
}
