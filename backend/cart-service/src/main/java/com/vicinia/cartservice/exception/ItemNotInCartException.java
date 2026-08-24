package com.vicinia.cartservice.exception;

import java.util.UUID;

public class ItemNotInCartException extends RuntimeException {
    public ItemNotInCartException(UUID listingId) {
        super("No such item in your cart: " + listingId);
    }
}
