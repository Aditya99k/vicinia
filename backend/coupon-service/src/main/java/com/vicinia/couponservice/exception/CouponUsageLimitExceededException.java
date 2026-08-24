package com.vicinia.couponservice.exception;

public class CouponUsageLimitExceededException extends RuntimeException {
    public CouponUsageLimitExceededException(String code) {
        super("Coupon " + code + " has reached its usage limit");
    }
}
