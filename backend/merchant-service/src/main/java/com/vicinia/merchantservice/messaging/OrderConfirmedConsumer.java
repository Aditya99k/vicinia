package com.vicinia.merchantservice.messaging;

import com.vicinia.merchantservice.domain.MerchantOrderTask;
import com.vicinia.merchantservice.repository.MerchantOrderTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * merchant-service's first Kafka consumer. Creates a local order task the
 * moment an order is confirmed and paid, so the owning merchant has
 * something to accept/reject — a third consumer group on order-events
 * alongside cart-service and payment-service, hence the explicit,
 * service-qualified retry/DLT suffixes (Stage 10's collision fix applies
 * here from day one, not retrofitted later).
 */
@Component
public class OrderConfirmedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmedConsumer.class);

    private final MerchantOrderTaskRepository taskRepository;

    public OrderConfirmedConsumer(MerchantOrderTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 2000, multiplier = 2.0), autoCreateTopics = "true",
            retryTopicSuffix = "-merchant-service-retry", dltTopicSuffix = "-merchant-service-dlt")
    @KafkaListener(topics = "order-events", groupId = "merchant-service")
    public void onOrderEvent(OrderEventEnvelope envelope) {
        if (!"order.confirmed".equals(envelope.eventType())) {
            return;
        }
        UUID orderId = UUID.fromString((String) envelope.payload().get("orderId"));
        UUID merchantId = UUID.fromString((String) envelope.payload().get("merchantId"));

        if (taskRepository.findByOrderId(orderId).isPresent()) {
            log.debug("Order task for {} already exists — skipping duplicate order.confirmed", orderId);
            return;
        }
        taskRepository.save(new MerchantOrderTask(orderId, merchantId));
        log.info("Created order task for merchant {} order {}", merchantId, orderId);
    }

    @DltHandler
    public void onDlt(OrderEventEnvelope envelope) {
        log.error("order-events message moved to DLQ after exhausting retries: eventId={} eventType={}",
                envelope.eventId(), envelope.eventType());
    }
}
