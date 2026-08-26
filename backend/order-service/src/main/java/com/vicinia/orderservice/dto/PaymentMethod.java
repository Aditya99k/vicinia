package com.vicinia.orderservice.dto;

/**
 * WALLET resolves synchronously in the same placeOrder call; RAZORPAY
 * leaves the order PAYMENT_PENDING and resolves later via
 * PaymentEventConsumer, once the webhook (or the client-side verify
 * fallback) fires (ARCHITECTURE.md §4.6/§8 — genuinely async by nature).
 * COD also resolves synchronously like WALLET (nothing to wait on — there
 * is no upfront charge at all), but never becomes Order.paid through this
 * app; cash/UPI is collected in person by the delivery partner, outside
 * anything this system tracks.
 */
public enum PaymentMethod {
    WALLET,
    RAZORPAY,
    COD
}
