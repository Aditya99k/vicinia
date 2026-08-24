package com.vicinia.orderservice.dto;

import com.vicinia.orderservice.domain.Order;

/** razorpayOrderId/razorpayKeyId are null for a wallet checkout — everything a real frontend needs to open Razorpay's Checkout.js widget when they're not. */
public record PlaceOrderResult(Order order, String razorpayOrderId, String razorpayKeyId) {
    public static PlaceOrderResult wallet(Order order) {
        return new PlaceOrderResult(order, null, null);
    }
}
