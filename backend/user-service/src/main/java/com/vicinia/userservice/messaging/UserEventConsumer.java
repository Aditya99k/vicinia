package com.vicinia.userservice.messaging;

import com.vicinia.userservice.domain.UserProfile;
import com.vicinia.userservice.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Reacts to auth-service's user-events topic. Idempotent by construction
 * (ARCHITECTURE.md §11): checks current state before applying rather than
 * relying on a separate processed_events table, since "does a profile
 * already exist for this userId" is itself the natural idempotency check.
 */
@Component
public class UserEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);

    private final UserProfileRepository userProfileRepository;

    public UserEventConsumer(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @KafkaListener(topics = "user-events", groupId = "user-service")
    public void onUserEvent(UserEventEnvelope envelope) {
        switch (envelope.eventType()) {
            case "user.registered" -> handleUserRegistered(envelope);
            case "user.deleted" -> log.debug("user.deleted received for {} — no delete flow yet, no-op", envelope.eventId());
            default -> log.debug("Ignoring unrecognized eventType '{}'", envelope.eventType());
        }
    }

    private void handleUserRegistered(UserEventEnvelope envelope) {
        String userId = (String) envelope.payload().get("userId");
        String email = (String) envelope.payload().get("email");
        UUID id = UUID.fromString(userId);

        if (userProfileRepository.existsById(id)) {
            log.debug("Profile for {} already exists — skipping duplicate user.registered", userId);
            return;
        }
        userProfileRepository.save(new UserProfile(id, email));
        log.info("Created profile for {}", userId);
    }
}
