package com.vicinia.paymentservice.web;

import com.vicinia.paymentservice.dto.RazorpayOrderRequest;
import com.vicinia.paymentservice.dto.RazorpayOrderResponse;
import com.vicinia.paymentservice.service.RazorpayPaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal-only, same trust model as InternalPaymentController's /pay and /refund — the real caller is order-service, gated by InternalRequestFilter alone. */
@RestController
@RequestMapping("/api/payments/razorpay")
public class InternalRazorpayController {

    private final RazorpayPaymentService razorpayPaymentService;

    public InternalRazorpayController(RazorpayPaymentService razorpayPaymentService) {
        this.razorpayPaymentService = razorpayPaymentService;
    }

    @PostMapping("/order")
    public ResponseEntity<RazorpayOrderResponse> createOrder(@Valid @RequestBody RazorpayOrderRequest request) {
        var response = razorpayPaymentService.createOrder(request.userId(), request.orderId(), request.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
