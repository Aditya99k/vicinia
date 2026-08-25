package com.vicinia.reviewservice.exception;

public class ReviewAlreadyExistsException extends RuntimeException {
    public ReviewAlreadyExistsException() {
        super("You've already reviewed this product");
    }
}
