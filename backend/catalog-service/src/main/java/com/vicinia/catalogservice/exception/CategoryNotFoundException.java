package com.vicinia.catalogservice.exception;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(String category) {
        super("Unknown category '" + category + "' — see GET /api/catalog/categories for valid values");
    }
}
