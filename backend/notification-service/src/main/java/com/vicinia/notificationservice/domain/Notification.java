package com.vicinia.notificationservice.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A record of one notification the system decided to send, keyed for
 * idempotency by the source Kafka event's own eventId rather than a
 * separate processed_events table (ARCHITECTURE.md §11's preferred
 * approach) — "does a notification for this eventId already exist" is the
 * natural state-check-before-mutating here, the same shape as every other
 * consumer's idempotency guard in this project, just applied to the one
 * piece of state this service actually owns. recipientUserId is nullable:
 * inventory.low is addressed to the merchant (their ownerUserId, per the
 * system-wide merchantId convention), which is still a userId, but some
 * future event type might not have a single clear recipient at all.
 */
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    private String eventId;
    private String eventType;
    private String recipientUserId;
    private String channel = "EMAIL";
    private String subject;
    private String body;

    private Instant createdAt = Instant.now();

    protected Notification() {
    }

    public Notification(String eventId, String eventType, String recipientUserId, String subject, String body) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.recipientUserId = recipientUserId;
        this.subject = subject;
        this.body = body;
    }

    public String getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getRecipientUserId() {
        return recipientUserId;
    }

    public String getChannel() {
        return channel;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
