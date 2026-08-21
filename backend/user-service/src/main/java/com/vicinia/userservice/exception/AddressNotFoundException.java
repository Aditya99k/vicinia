package com.vicinia.userservice.exception;

public class AddressNotFoundException extends RuntimeException {
    public AddressNotFoundException(String addressId) {
        super("No address " + addressId + " for this user");
    }
}
