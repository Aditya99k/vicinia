package com.vicinia.paymentservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Everything a real frontend needs to open Razorpay's Checkout.js widget — keyId is the public key, safe to hand to a browser. */
public record RazorpayOrderResponse(
        UUID orderId,
        String razorpayOrderId,
        String razorpayKeyId,
        BigDecimal amount,
        String currency
) {
}
