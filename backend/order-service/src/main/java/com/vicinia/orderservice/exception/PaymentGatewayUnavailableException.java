package com.vicinia.orderservice.exception;

/** The Razorpay circuit breaker is open, or its bulkhead is full (Stage 15) — the customer should try wallet payment instead. */
public class PaymentGatewayUnavailableException extends RuntimeException {
    public PaymentGatewayUnavailableException(Throwable cause) {
        super("Card/UPI payment is temporarily unavailable — please try wallet payment", cause);
    }
}
