package com.vicinia.orderservice.dto;

/** couponCode is optional — a plain checkout with no coupon is the common case. paymentMethod defaults to WALLET when omitted, preserving Stage 8's original synchronous behavior unchanged. */
public record PlaceOrderRequest(
        String couponCode,
        PaymentMethod paymentMethod
) {
}
