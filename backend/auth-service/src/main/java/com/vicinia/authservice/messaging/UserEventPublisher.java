package com.vicinia.authservice.messaging;

import com.vicinia.common.event.EventEnvelope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Publishes to the user-events topic (ARCHITECTURE.md §9 — one topic per
 * aggregate, partitioned by userId so every event for one user is ordered).
 */
@Component
public class UserEventPublisher {

    private static final String TOPIC = "user-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserRegistered(String userId, String email, Set<String> roles) {
        var payload = new UserRegisteredPayload(userId, email, roles);
        var envelope = EventEnvelope.of("user.registered", payload);
        kafkaTemplate.send(TOPIC, userId, envelope);
    }

    public void publishUserDeleted(String userId) {
        var envelope = EventEnvelope.of("user.deleted", new UserDeletedPayload(userId));
        kafkaTemplate.send(TOPIC, userId, envelope);
    }

    public record UserDeletedPayload(String userId) {
    }
}
