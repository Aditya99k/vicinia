package com.vicinia.merchantservice.messaging;

import com.vicinia.merchantservice.domain.MerchantOrderTask;
import com.vicinia.merchantservice.repository.MerchantOrderTaskRepository;
import com.vicinia.merchantservice.service.MerchantOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * merchant-service's one and only consumer group on order-events — a third
 * alongside cart-service's and payment-service's, hence the explicit,
 * service-qualified retry/DLT suffixes (Stage 10's collision fix applies
 * here from day one, not retrofitted later). Both order.confirmed (create
 * a task) and order.cancelled (Stage 18: clear a stale one out of the
 * merchant's queue after their own cancel) are handled in this single
 * method rather than two separate @KafkaListener methods — two listeners
 * with the same groupId on the same topic would split that topic's
 * partitions between them instead of both seeing every message, the same
 * "one listener per topic per service" rule every other consumer in this
 * project already follows (PaymentEventConsumer/DeliveryEventConsumer in
 * order-service branch on eventType internally for exactly this reason).
 */
@Component
public class OrderConfirmedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmedConsumer.class);

    private final MerchantOrderTaskRepository taskRepository;
    private final MerchantOrderService merchantOrderService;

    public OrderConfirmedConsumer(MerchantOrderTaskRepository taskRepository, MerchantOrderService merchantOrderService) {
        this.taskRepository = taskRepository;
        this.merchantOrderService = merchantOrderService;
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 2.0), autoCreateTopics = "true",
            retryTopicSuffix = "-merchant-service-retry", dltTopicSuffix = "-merchant-service-dlt")
    @KafkaListener(topics = "order-events", groupId = "merchant-service")
    public void onOrderEvent(OrderEventEnvelope envelope) {
        UUID orderId = UUID.fromString((String) envelope.payload().get("orderId"));

        switch (envelope.eventType()) {
            case "order.confirmed" -> {
                UUID merchantId = UUID.fromString((String) envelope.payload().get("merchantId"));
                if (taskRepository.findByOrderId(orderId).isPresent()) {
                    log.debug("Order task for {} already exists — skipping duplicate order.confirmed", orderId);
                    return;
                }
                taskRepository.save(new MerchantOrderTask(orderId, merchantId));
                log.info("Created order task for merchant {} order {}", merchantId, orderId);
            }
            case "order.cancelled" -> merchantOrderService.cancelFromOrderEvent(orderId);
            default -> log.debug("Ignoring unrecognized eventType '{}'", envelope.eventType());
        }
    }

    @DltHandler
    public void onDlt(OrderEventEnvelope envelope) {
        log.error("order-events message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
    }
}
