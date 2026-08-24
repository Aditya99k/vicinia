package com.vicinia.orderservice.dto;

/** WALLET resolves synchronously in the same placeOrder call; RAZORPAY leaves the order PAYMENT_PENDING and resolves later via PaymentEventConsumer, once the webhook fires (ARCHITECTURE.md §4.6/§8 — genuinely async by nature). */
public enum PaymentMethod {
    WALLET,
    RAZORPAY
}
