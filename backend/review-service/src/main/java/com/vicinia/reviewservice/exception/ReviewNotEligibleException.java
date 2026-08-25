package com.vicinia.reviewservice.exception;

public class ReviewNotEligibleException extends RuntimeException {
    public ReviewNotEligibleException() {
        super("You can only review a product after a delivered order containing it");
    }
}
