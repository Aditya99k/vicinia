package com.vicinia.paymentservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.paymentservice.dto.RazorpayVerifyRequest;
import com.vicinia.paymentservice.service.RazorpayPaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Customer-facing (unlike RazorpayWebhookController's anonymous trust
 * model) — called directly by the browser from checkout.js's own success
 * handler, so a real order-service confirmation doesn't depend on
 * Razorpay's servers being able to reach this stack's webhook at all. See
 * RazorpayPaymentService.verifyAndResolve for why this is exactly as
 * trustworthy as the webhook path.
 */
@RestController
@RequestMapping("/api/payments/razorpay")
public class RazorpayVerifyController {

    private final RazorpayPaymentService razorpayPaymentService;

    public RazorpayVerifyController(RazorpayPaymentService razorpayPaymentService) {
        this.razorpayPaymentService = razorpayPaymentService;
    }

    @PostMapping("/verify")
    public void verify(@RequestHeader(HeaderNames.USER_ID) String userId, @Valid @RequestBody RazorpayVerifyRequest request) {
        razorpayPaymentService.verifyAndResolve(
                UUID.fromString(userId), request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature());
    }
}
