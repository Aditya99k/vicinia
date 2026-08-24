package com.vicinia.orderservice.dto;

/** couponCode is optional — a plain checkout with no coupon is the common case. */
public record PlaceOrderRequest(
        String couponCode
) {
}
