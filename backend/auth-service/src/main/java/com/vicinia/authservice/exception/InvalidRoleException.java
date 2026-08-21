package com.vicinia.authservice.exception;

public class InvalidRoleException extends RuntimeException {
    public InvalidRoleException(String role) {
        super("'" + role + "' is not a self-registerable role");
    }
}
