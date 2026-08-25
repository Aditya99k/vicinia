package com.vicinia.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * No real email/SMS provider is wired up yet (BUILD_TRACKER.md Stage 12:
 * "Email send (or logged-only stub if no real provider wired)") — this is
 * that stub. Logging at INFO is the "send" for now; swapping in a real
 * provider later is a new NotificationSender implementation, not a change
 * to NotificationService or any consumer.
 */
@Component
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(String recipientUserId, String subject, String body) {
        log.info("NOTIFICATION → recipient={} subject=\"{}\" body=\"{}\"", recipientUserId, subject, body);
    }
}
