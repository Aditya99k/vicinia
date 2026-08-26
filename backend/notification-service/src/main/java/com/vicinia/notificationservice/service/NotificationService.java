package com.vicinia.notificationservice.service;

import com.vicinia.notificationservice.domain.Notification;
import com.vicinia.notificationservice.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationSender sender;

    public NotificationService(NotificationRepository repository, NotificationSender sender) {
        this.repository = repository;
        this.sender = sender;
    }

    /** Idempotent on eventId — a redelivered Kafka message (retry, rebalance) never produces a second notification. */
    public void record(String eventId, String eventType, String recipientUserId, String subject, String body, String referenceId) {
        if (repository.existsByEventId(eventId)) {
            return;
        }
        sender.send(recipientUserId, subject, body);
        repository.save(new Notification(eventId, eventType, recipientUserId, subject, body, referenceId));
    }

    public List<Notification> mine(String recipientUserId) {
        return repository.findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId);
    }
}
