package com.vicinia.couponservice.exception;

public class CouponAlreadyUsedByUserException extends RuntimeException {
    public CouponAlreadyUsedByUserException(String code) {
        super("You've already used coupon " + code + " the maximum number of times");
    }
}
