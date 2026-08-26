package com.vicinia.notificationservice.dto;

import com.vicinia.notificationservice.domain.Notification;

import java.time.Instant;

public record NotificationResponse(
        String id,
        String eventType,
        String channel,
        String subject,
        String body,
        String referenceId,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getEventType(),
                notification.getChannel(),
                notification.getSubject(),
                notification.getBody(),
                notification.getReferenceId(),
                notification.getCreatedAt()
        );
    }
}
