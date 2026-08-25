package com.vicinia.notificationservice.service;

/**
 * The actual delivery mechanism, kept separate from NotificationService's
 * idempotency/persistence logic so a real email/SMS provider can be dropped
 * in later (Stage 20-style deployment concern) without touching anything
 * that decides *what* to send or *whether* it's already been sent.
 */
public interface NotificationSender {

    void send(String recipientUserId, String subject, String body);
}
