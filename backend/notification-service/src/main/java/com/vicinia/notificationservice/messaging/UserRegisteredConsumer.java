package com.vicinia.notificationservice.messaging;

import com.vicinia.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * First of notification-service's 4 consumers (ARCHITECTURE.md §7's
 * table). One consumer group per service across all 4 topics, but each
 * listener still gets its own explicit, service-qualified retry/DLT
 * suffix — Stage 10's collision fix, applied from day one rather than
 * retrofitted after a real collision.
 */
@Component
public class UserRegisteredConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredConsumer.class);

    private final NotificationService notificationService;

    public UserRegisteredConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 2.0), autoCreateTopics = "true",
            retryTopicSuffix = "-notification-service-user-retry", dltTopicSuffix = "-notification-service-user-dlt")
    @KafkaListener(topics = "user-events", groupId = "notification-service")
    public void onUserEvent(IncomingEventEnvelope envelope) {
        if (!"user.registered".equals(envelope.eventType())) {
            return;
        }
        String userId = (String) envelope.payload().get("userId");
        String email = (String) envelope.payload().get("email");
        notificationService.record(envelope.eventId(), envelope.eventType(), userId,
                "Welcome to Vicinia",
                "Hi, your account (" + email + ") is ready. Start browsing nearby stores.");
    }

    @DltHandler
    public void onDlt(IncomingEventEnvelope envelope) {
        log.error("user-events message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
    }
}
