package com.vicinia.catalogservice.exception;

public class CategoryAlreadyExistsException extends RuntimeException {
    public CategoryAlreadyExistsException(String name) {
        super("Category '" + name + "' already exists");
    }
}
