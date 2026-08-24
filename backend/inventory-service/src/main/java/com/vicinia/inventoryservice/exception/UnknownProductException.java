package com.vicinia.inventoryservice.exception;

public class UnknownProductException extends RuntimeException {
    public UnknownProductException(String productId) {
        super("Unknown or unapproved product: " + productId);
    }
}
