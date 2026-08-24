package com.vicinia.couponservice.exception;

import java.math.BigDecimal;

public class MinOrderValueNotMetException extends RuntimeException {
    public MinOrderValueNotMetException(String code, BigDecimal minOrderValue) {
        super("Coupon " + code + " requires a minimum order value of " + minOrderValue);
    }
}
