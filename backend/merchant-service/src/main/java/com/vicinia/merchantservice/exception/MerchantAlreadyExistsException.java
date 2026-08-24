package com.vicinia.merchantservice.exception;

public class MerchantAlreadyExistsException extends RuntimeException {
    public MerchantAlreadyExistsException() {
        super("You already have a merchant application on file");
    }
}
