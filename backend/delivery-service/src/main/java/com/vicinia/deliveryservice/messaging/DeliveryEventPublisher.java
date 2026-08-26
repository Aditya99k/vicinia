package com.vicinia.deliveryservice.messaging;

import com.vicinia.common.event.EventEnvelope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Publishes to delivery-events, partitioned by orderId — order-service is the one real consumer (ARCHITECTURE.md §7/§8: "delivery status changes over minutes/hours; order-service just mirrors it"). */
@Component
public class DeliveryEventPublisher {

    private static final String TOPIC = "delivery-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DeliveryEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAssigned(UUID orderId, UUID partnerId) {
        var payload = new AssignedPayload(orderId.toString(), partnerId.toString());
        kafkaTemplate.send(TOPIC, orderId.toString(), EventEnvelope.of("delivery.assigned", payload));
    }

    public void publishPickedUp(UUID orderId) {
        kafkaTemplate.send(TOPIC, orderId.toString(), EventEnvelope.of("delivery.picked_up", new OrderIdPayload(orderId.toString())));
    }

    public void publishDelivered(UUID orderId) {
        kafkaTemplate.send(TOPIC, orderId.toString(), EventEnvelope.of("delivery.delivered", new OrderIdPayload(orderId.toString())));
    }

    public record AssignedPayload(String orderId, String partnerId) {
    }

    public record OrderIdPayload(String orderId) {
    }
}
