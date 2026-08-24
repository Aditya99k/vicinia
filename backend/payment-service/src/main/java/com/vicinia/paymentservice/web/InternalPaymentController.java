package com.vicinia.paymentservice.web;

import com.vicinia.paymentservice.dto.PayRequest;
import com.vicinia.paymentservice.dto.RefundRequest;
import com.vicinia.paymentservice.dto.TransactionResponse;
import com.vicinia.paymentservice.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal-only, matching inventory-service's ReservationController pattern
 * exactly (Stage 5): no user permission check, because the real caller is
 * never a person — it's order-service. InternalRequestFilter (checked on
 * every request regardless of path) is the only gate: a caller needs
 * X-Internal-Secret, not a JWT. Test directly against this service's own
 * port, not through api-gateway.
 */
@RestController
@RequestMapping("/api/payments/wallet")
public class InternalPaymentController {

    private final WalletService walletService;

    public InternalPaymentController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/pay")
    public ResponseEntity<TransactionResponse> pay(@Valid @RequestBody PayRequest request) {
        var transaction = walletService.pay(request.userId(), request.orderId(), request.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(transaction));
    }

    @PostMapping("/refund")
    public ResponseEntity<TransactionResponse> refund(@Valid @RequestBody RefundRequest request) {
        return walletService.refundIfPaid(request.orderId(), request.userId(), request.amount())
                .map(transaction -> ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(transaction)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
