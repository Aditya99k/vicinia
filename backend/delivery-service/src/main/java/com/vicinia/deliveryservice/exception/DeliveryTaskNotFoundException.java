package com.vicinia.deliveryservice.exception;

import java.util.UUID;

public class DeliveryTaskNotFoundException extends RuntimeException {
    public DeliveryTaskNotFoundException(UUID orderId) {
        super("No delivery task for order: " + orderId);
    }
}
