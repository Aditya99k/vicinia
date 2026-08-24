package com.vicinia.paymentservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.paymentservice.dto.TopupRequest;
import com.vicinia.paymentservice.dto.TransactionResponse;
import com.vicinia.paymentservice.dto.WalletResponse;
import com.vicinia.paymentservice.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Customer-facing, gateway-routed: a user checking/topping-up their own wallet. Nothing here is public. */
@RestController
@RequestMapping("/api/payments/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/balance")
    public WalletResponse balance(@RequestHeader(HeaderNames.USER_ID) String userId) {
        return WalletResponse.from(walletService.getByUserId(UUID.fromString(userId)));
    }

    @PostMapping("/topup")
    public ResponseEntity<TransactionResponse> topup(@RequestHeader(HeaderNames.USER_ID) String userId,
                                                       @Valid @RequestBody TopupRequest request) {
        var transaction = walletService.topup(UUID.fromString(userId), request.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(transaction));
    }
}
