package com.vicinia.inventoryservice.exception;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(UUID listingId) {
        super("Insufficient stock for listing " + listingId);
    }
}
