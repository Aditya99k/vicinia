package com.vicinia.orderservice.exception;

public class CartHasUnavailableItemsException extends RuntimeException {
    public CartHasUnavailableItemsException() {
        super("Your cart has items that are no longer available — remove them before checking out");
    }
}
