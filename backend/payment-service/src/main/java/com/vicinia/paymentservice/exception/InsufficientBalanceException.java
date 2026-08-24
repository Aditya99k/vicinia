package com.vicinia.paymentservice.exception;

import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(UUID userId) {
        super("Insufficient wallet balance for user: " + userId);
    }
}
