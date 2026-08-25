package com.vicinia.settlementservice.messaging;

import com.vicinia.settlementservice.service.SettlementService;
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
 * order-events also carries order.created/confirmed/cancelled and Stage
 * 11's merchant.accepted/rejected and order.ready — this consumer only
 * cares about order.delivered (Stage 14, ARCHITECTURE.md §4.7 — triggered
 * on DELIVERED specifically, not CONFIRMED, so a cancellation/refund
 * between confirm and delivery never has to claw back a settlement).
 */
@Component
public class OrderDeliveredConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderDeliveredConsumer.class);

    private final SettlementService settlementService;

    public OrderDeliveredConsumer(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 2.0), autoCreateTopics = "true",
            retryTopicSuffix = "-settlement-service-retry", dltTopicSuffix = "-settlement-service-dlt")
    @KafkaListener(topics = "order-events", groupId = "settlement-service")
    public void onOrderEvent(IncomingEventEnvelope envelope) {
        if (!"order.delivered".equals(envelope.eventType())) {
            return;
        }
        UUID orderId = UUID.fromString((String) envelope.payload().get("orderId"));
        UUID merchantId = UUID.fromString((String) envelope.payload().get("merchantId"));
        BigDecimal totalAmount = new BigDecimal((String) envelope.payload().get("totalAmount"));
        settlementService.recordDelivered(orderId, merchantId, totalAmount);
    }

    @DltHandler
    public void onDlt(IncomingEventEnvelope envelope) {
        log.error("order-events message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
    }
}
