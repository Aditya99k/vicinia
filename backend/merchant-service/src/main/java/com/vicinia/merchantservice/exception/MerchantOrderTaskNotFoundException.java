package com.vicinia.merchantservice.exception;

import java.util.UUID;

public class MerchantOrderTaskNotFoundException extends RuntimeException {
    public MerchantOrderTaskNotFoundException(UUID orderId) {
        super("No order task for order: " + orderId);
    }
}
