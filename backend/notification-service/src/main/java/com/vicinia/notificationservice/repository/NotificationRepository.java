package com.vicinia.notificationservice.repository;

import com.vicinia.notificationservice.domain.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    boolean existsByEventId(String eventId);

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId);
}
