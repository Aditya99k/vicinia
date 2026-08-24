package com.vicinia.paymentservice.exception;

import java.util.UUID;

public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(UUID userId) {
        super("No wallet for user: " + userId);
    }
}
