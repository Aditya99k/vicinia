package com.vicinia.paymentservice.exception;

public class RazorpayPaymentNotFoundException extends RuntimeException {
    public RazorpayPaymentNotFoundException(String razorpayOrderId) {
        super("No payment record for Razorpay order: " + razorpayOrderId);
    }
}
