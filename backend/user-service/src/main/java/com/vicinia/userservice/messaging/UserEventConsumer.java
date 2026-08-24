package com.vicinia.userservice.messaging;

import com.vicinia.userservice.domain.UserProfile;
import com.vicinia.userservice.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Reacts to auth-service's user-events topic. Idempotent by construction
 * (ARCHITECTURE.md §11): checks current state before applying rather than
 * relying on a separate processed_events table, since "does a profile
 * already exist for this userId" is itself the natural idempotency check.
 *
 * @RetryableTopic (Stage 10, ARCHITECTURE.md §9): a failure retries via
 * non-blocking retry topics, not a blocking Thread.sleep in this listener —
 * the container keeps consuming new messages on the main topic while a
 * failed one waits its turn on a retry topic. After 4 attempts, it lands
 * on a DLT instead of being silently dropped. Explicit, service-qualified
 * retry/DLT suffixes — not Spring Kafka's default (source-topic-based)
 * naming — because payment-service's PlatformEventConsumer also applies
 * @RetryableTopic to this same user-events topic; with the default naming
 * both consumer groups would collide on one shared "user-events-dlt",
 * meaning a message dead-lettered by one service's failure would also
 * trigger the other service's unrelated @DltHandler. Found by actually
 * triggering a DLQ failure and seeing it show up in payment-service's
 * logs too, not by inspection — see BUILD_TRACKER.md's Stage 10 notes.
 */
@Component
public class UserEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);

    private final UserProfileRepository userProfileRepository;

    public UserEventConsumer(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 2.0), autoCreateTopics = "true",
            retryTopicSuffix = "-user-service-retry", dltTopicSuffix = "-user-service-dlt")
    @KafkaListener(topics = "user-events", groupId = "user-service")
    public void onUserEvent(UserEventEnvelope envelope) {
        switch (envelope.eventType()) {
            case "user.registered" -> handleUserRegistered(envelope);
            case "user.deleted" -> log.debug("user.deleted received for {} — no delete flow yet, no-op", envelope.eventId());
            default -> log.debug("Ignoring unrecognized eventType '{}'", envelope.eventType());
        }
    }

    @DltHandler
    public void onDlt(UserEventEnvelope envelope) {
        log.error("user-events message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
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
