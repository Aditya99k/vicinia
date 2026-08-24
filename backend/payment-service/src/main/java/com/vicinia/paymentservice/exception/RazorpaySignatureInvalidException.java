package com.vicinia.paymentservice.exception;

public class RazorpaySignatureInvalidException extends RuntimeException {
    public RazorpaySignatureInvalidException() {
        super("Invalid Razorpay webhook signature");
    }
}
