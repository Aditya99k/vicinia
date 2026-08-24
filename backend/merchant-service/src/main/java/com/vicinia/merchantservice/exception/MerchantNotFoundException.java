package com.vicinia.merchantservice.exception;

public class MerchantNotFoundException extends RuntimeException {
    public MerchantNotFoundException(String id) {
        super("No merchant " + id);
    }

    public MerchantNotFoundException() {
        super("No merchant application found for this account");
    }
}
