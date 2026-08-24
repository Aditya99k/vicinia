package com.vicinia.couponservice.exception;

/** Covers inactive, not-yet-started, and expired — all "this code doesn't currently work" from the caller's point of view. */
public class CouponNotActiveException extends RuntimeException {
    public CouponNotActiveException(String code) {
        super("Coupon is not currently active: " + code);
    }
}
