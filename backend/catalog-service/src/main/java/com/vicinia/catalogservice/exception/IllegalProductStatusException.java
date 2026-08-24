package com.vicinia.catalogservice.exception;

public class IllegalProductStatusException extends RuntimeException {
    public IllegalProductStatusException(String message) {
        super(message);
    }
}
