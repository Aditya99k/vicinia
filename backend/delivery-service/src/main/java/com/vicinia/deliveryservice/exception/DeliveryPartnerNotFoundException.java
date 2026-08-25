package com.vicinia.deliveryservice.exception;

import java.util.UUID;

public class DeliveryPartnerNotFoundException extends RuntimeException {
    public DeliveryPartnerNotFoundException(UUID userId) {
        super("No delivery partner record for user: " + userId);
    }
}
