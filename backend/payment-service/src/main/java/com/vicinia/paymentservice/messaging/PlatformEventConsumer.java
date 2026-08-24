package com.vicinia.paymentservice.messaging;

import com.vicinia.paymentservice.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Two independent reasons to listen, sharing one consumer since both are
 * simple, low-volume, and payment-service is small enough not to need
 * separate listener classes per topic:
 *
 * <p>user.registered — auto-provisions a Wallet, mirroring user-service's
 * own UserProfile auto-creation from Stage 2. earliest offset (see
 * application.yml) backfills every user that existed before this service
 * did.
 *
 * <p>order.cancelled — per ARCHITECTURE.md §7's dependency table,
 * payment-service is the one service that actually consumes this: if the
 * cancelled order was already paid, refund it. This is genuinely
 * event-driven by design (§8's REST-vs-Kafka rule): order-service doesn't
 * need the refund's result to decide what to do next, it just needs the
 * cancellation to eventually be reflected in the customer's balance.
 */
@Component
public class PlatformEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PlatformEventConsumer.class);

    private final WalletService walletService;

    public PlatformEventConsumer(WalletService walletService) {
        this.walletService = walletService;
    }

    @KafkaListener(topics = {"user-events", "order-events"}, groupId = "payment-service")
    public void onEvent(PlatformEventEnvelope envelope) {
        switch (envelope.eventType()) {
            case "user.registered" -> handleUserRegistered(envelope);
            case "order.cancelled" -> handleOrderCancelled(envelope);
            default -> log.debug("Ignoring unrecognized eventType '{}'", envelope.eventType());
        }
    }

    private void handleUserRegistered(PlatformEventEnvelope envelope) {
        UUID userId = UUID.fromString((String) envelope.payload().get("userId"));
        walletService.provisionIfAbsent(userId);
    }

    private void handleOrderCancelled(PlatformEventEnvelope envelope) {
        UUID orderId = UUID.fromString((String) envelope.payload().get("orderId"));
        UUID userId = UUID.fromString((String) envelope.payload().get("userId"));
        BigDecimal amount = new BigDecimal((String) envelope.payload().get("totalAmount"));
        walletService.refundIfPaid(orderId, userId, amount);
    }
}
