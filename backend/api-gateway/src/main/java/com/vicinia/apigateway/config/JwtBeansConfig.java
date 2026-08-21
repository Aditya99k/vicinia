package com.vicinia.apigateway.config;

import com.vicinia.common.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Same secret + TTL as auth-service (both pulled from config-server) — this is what lets the gateway verify tokens auth-service issued. */
@Configuration
public class JwtBeansConfig {

    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${vicinia.jwt.secret}") String secret,
            @Value("${vicinia.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes) {
        return new JwtTokenProvider(secret, accessTokenTtlMinutes);
    }
}
