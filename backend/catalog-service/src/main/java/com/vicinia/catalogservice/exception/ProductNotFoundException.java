package com.vicinia.catalogservice.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String id) {
        super("No product " + id);
    }
}
