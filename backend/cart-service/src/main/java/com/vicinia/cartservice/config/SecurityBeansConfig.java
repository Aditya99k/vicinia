package com.vicinia.cartservice.config;

import com.vicinia.common.observability.CorrelationIdSupport;
import com.vicinia.common.security.CorrelationIdFilter;
import com.vicinia.common.security.InternalRequestFilter;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityBeansConfig {

    @Bean
    public FilterRegistrationBean<InternalRequestFilter> internalRequestFilter(
            @Value("${vicinia.internal-secret}") String secret) {
        FilterRegistrationBean<InternalRequestFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new InternalRequestFilter(secret));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }

    /** Stage 16 -- runs before InternalRequestFilter (order 0 vs 1) so even a request that filter rejects still logs with a real correlation ID. */
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CorrelationIdFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(0);
        return registration;
    }

    /** Stage 16 -- MDC correlation ID for every @KafkaListener invocation, matching CorrelationIdFilter's HTTP-side equivalent. */
    @Bean
    public ContainerCustomizer<Object, Object, ConcurrentMessageListenerContainer<Object, Object>> correlationIdContainerCustomizer() {
        return CorrelationIdSupport.containerCustomizer();
    }
}
