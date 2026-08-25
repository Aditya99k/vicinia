package com.vicinia.deliveryservice.messaging;

import com.vicinia.deliveryservice.service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.UUID;

/** Auto-provisions a DeliveryPartner record for any user who signed up with the DELIVERY_PARTNER role — mirroring payment-service's wallet auto-provisioning (Stage 8) and user-service's own profile auto-creation (Stage 2). A fourth consumer group on user-events, hence the explicit retry/DLT suffix. */
@Component
public class UserRegisteredConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredConsumer.class);

    private final DeliveryService deliveryService;

    public UserRegisteredConsumer(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 2.0), autoCreateTopics = "true",
            retryTopicSuffix = "-delivery-service-user-retry", dltTopicSuffix = "-delivery-service-user-dlt")
    @KafkaListener(topics = "user-events", groupId = "delivery-service")
    public void onUserEvent(IncomingEventEnvelope envelope) {
        if (!"user.registered".equals(envelope.eventType())) {
            return;
        }
        Object rolesValue = envelope.payload().get("roles");
        if (!(rolesValue instanceof Collection<?> roles) || !roles.contains("DELIVERY_PARTNER")) {
            return;
        }
        UUID userId = UUID.fromString((String) envelope.payload().get("userId"));
        deliveryService.provisionIfAbsent(userId);
    }

    @DltHandler
    public void onDlt(IncomingEventEnvelope envelope) {
        log.error("user-events message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
    }
}
