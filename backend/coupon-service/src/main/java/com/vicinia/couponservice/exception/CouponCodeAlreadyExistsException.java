package com.vicinia.couponservice.exception;

public class CouponCodeAlreadyExistsException extends RuntimeException {
    public CouponCodeAlreadyExistsException(String code) {
        super("A coupon with code " + code + " already exists");
    }
}
