package com.vicinia.cartservice.exception;

/** ADR 0001 — one merchant per cart. The client must clear the cart before adding an item from a different store. */
public class DifferentMerchantException extends RuntimeException {
    public DifferentMerchantException() {
        super("Your cart has items from a different merchant — clear your cart before adding this item");
    }
}
