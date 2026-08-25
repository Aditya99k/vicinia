package com.vicinia.merchantservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.merchantservice.dto.MerchantOrderTaskResponse;
import com.vicinia.merchantservice.dto.ReasonRequest;
import com.vicinia.merchantservice.service.MerchantOrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** A merchant managing their own orders — ownership resolved from X-User-Id via merchantService.getMine, same pattern as MerchantController's /me endpoints. */
@RestController
@RequestMapping("/api/merchants/orders")
public class MerchantOrderController {

    private final MerchantOrderService merchantOrderService;

    public MerchantOrderController(MerchantOrderService merchantOrderService) {
        this.merchantOrderService = merchantOrderService;
    }

    @GetMapping("/pending")
    public List<MerchantOrderTaskResponse> pending(@RequestHeader(HeaderNames.USER_ID) String userId) {
        return merchantOrderService.pending(UUID.fromString(userId)).stream().map(MerchantOrderTaskResponse::from).toList();
    }

    @PostMapping("/{orderId}/accept")
    public MerchantOrderTaskResponse accept(@RequestHeader(HeaderNames.USER_ID) String userId, @PathVariable UUID orderId) {
        return MerchantOrderTaskResponse.from(merchantOrderService.accept(UUID.fromString(userId), orderId));
    }

    @PostMapping("/{orderId}/reject")
    public MerchantOrderTaskResponse reject(@RequestHeader(HeaderNames.USER_ID) String userId,
                                             @PathVariable UUID orderId,
                                             @Valid @RequestBody ReasonRequest request) {
        return MerchantOrderTaskResponse.from(merchantOrderService.reject(UUID.fromString(userId), orderId, request.reason()));
    }

    @PostMapping("/{orderId}/ready")
    public MerchantOrderTaskResponse ready(@RequestHeader(HeaderNames.USER_ID) String userId, @PathVariable UUID orderId) {
        return MerchantOrderTaskResponse.from(merchantOrderService.markReady(UUID.fromString(userId), orderId));
    }
}
