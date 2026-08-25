package com.vicinia.authservice.config;

import com.vicinia.common.jwt.JwtTokenProvider;
import com.vicinia.common.security.CorrelationIdFilter;
import com.vicinia.common.security.InternalRequestFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Wired explicitly here (constructor injection via @Value), not via
 * component-scanning `common` — see InternalRequestFilter's javadoc for why.
 */
@Configuration
public class SecurityBeansConfig {

    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${vicinia.jwt.secret}") String secret,
            @Value("${vicinia.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes) {
        return new JwtTokenProvider(secret, accessTokenTtlMinutes);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

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
}
