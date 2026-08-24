package com.vicinia.paymentservice.messaging;

import com.vicinia.paymentservice.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
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
 *
 * @RetryableTopic (Stage 10): one method, two source topics — Spring Kafka
 * builds an independent retry and DLT topic chain per source topic
 * (separately for user-events and for order-events), which is the correct
 * behavior here: a failure consuming a user.registered event shouldn't be
 * conflated with a failure consuming an order.cancelled event even though
 * one listener method handles both. Explicit, service-qualified retry/DLT
 * suffixes, not Spring Kafka's default naming — both source topics here
 * are also consumed by a different service's own @RetryableTopic listener
 * (user-service on user-events, cart-service on order-events), and the
 * default naming is source-topic-based, not consumer-group-based, so
 * without this every pair of consumer groups sharing a topic would collide
 * onto one shared DLT — confirmed by actually triggering a cart-service
 * failure and seeing it show up in this service's own @DltHandler logs
 * too, before this fix. See BUILD_TRACKER.md's Stage 10 notes.
 */
@Component
public class PlatformEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PlatformEventConsumer.class);

    private final WalletService walletService;

    public PlatformEventConsumer(WalletService walletService) {
        this.walletService = walletService;
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 2.0), autoCreateTopics = "true",
            retryTopicSuffix = "-payment-service-retry", dltTopicSuffix = "-payment-service-dlt")
    @KafkaListener(topics = {"user-events", "order-events"}, groupId = "payment-service")
    public void onEvent(PlatformEventEnvelope envelope) {
        switch (envelope.eventType()) {
            case "user.registered" -> handleUserRegistered(envelope);
            case "order.cancelled" -> handleOrderCancelled(envelope);
            default -> log.debug("Ignoring unrecognized eventType '{}'", envelope.eventType());
        }
    }

    @DltHandler
    public void onDlt(PlatformEventEnvelope envelope) {
        log.error("payment-service message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
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
