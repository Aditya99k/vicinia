package com.vicinia.paymentservice.web;

import com.vicinia.paymentservice.service.RazorpayPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A fundamentally different trust model from every other endpoint in this
 * project: Razorpay's own servers call this, not a logged-in user and not
 * another internal Vicinia service — it carries neither a JWT nor
 * X-Internal-Secret naturally. Routed through api-gateway like everything
 * else (added to public-paths, so AuthGlobalFilter skips the JWT check),
 * which still stamps X-Internal-Secret unconditionally on every proxied
 * request regardless of public/private path — so this passes
 * InternalRequestFilter with zero changes needed there. The real security
 * boundary here is entirely the HMAC-SHA256 signature check in
 * RazorpayPaymentService, not JWT or the internal secret.
 *
 * @RequestBody String, not a typed DTO — signature verification needs the
 * exact raw bytes Razorpay signed, not a re-serialized reconstruction.
 */
@RestController
@RequestMapping("/api/payments/razorpay")
public class RazorpayWebhookController {

    private final RazorpayPaymentService razorpayPaymentService;

    public RazorpayWebhookController(RazorpayPaymentService razorpayPaymentService) {
        this.razorpayPaymentService = razorpayPaymentService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String rawBody,
                                         @RequestHeader("X-Razorpay-Signature") String signature) {
        razorpayPaymentService.handleWebhook(rawBody, signature);
        return ResponseEntity.ok().build();
    }
}
