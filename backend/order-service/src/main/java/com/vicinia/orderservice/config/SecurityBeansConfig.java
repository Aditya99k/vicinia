package com.vicinia.orderservice.config;

import com.vicinia.common.security.InternalRequestFilter;
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
}
