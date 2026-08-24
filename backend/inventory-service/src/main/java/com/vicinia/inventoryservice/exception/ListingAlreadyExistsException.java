package com.vicinia.inventoryservice.exception;

public class ListingAlreadyExistsException extends RuntimeException {
    public ListingAlreadyExistsException() {
        super("You already have a listing for this product");
    }
}
