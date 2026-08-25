package com.vicinia.common.observability;

import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

/** One-line registration each Kafka-consuming service adds as a @Bean in its own SecurityBeansConfig — see any such service for the exact call. */
public final class CorrelationIdSupport {

    private CorrelationIdSupport() {
    }

    public static ContainerCustomizer<Object, Object, ConcurrentMessageListenerContainer<Object, Object>> containerCustomizer() {
        return container -> container.setRecordInterceptor(new CorrelationIdRecordInterceptor());
    }
}
