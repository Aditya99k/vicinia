package com.vicinia.settlementservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.settlementservice.dto.PayoutResponse;
import com.vicinia.settlementservice.dto.SettlementEntryResponse;
import com.vicinia.settlementservice.service.SettlementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** A merchant viewing their own ledger — self-scoped via X-User-Id (merchantId = the store owner's userId, the system-wide convention), no separate RBAC permission needed, same pattern as order-service's /mine. Base path matches api-gateway's route exactly: /api/settlements (plural), no path rewriting. */
@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping("/mine")
    public List<SettlementEntryResponse> mine(@RequestHeader(HeaderNames.USER_ID) String userId) {
        return settlementService.mine(UUID.fromString(userId)).stream().map(SettlementEntryResponse::from).toList();
    }

    @GetMapping("/payouts/mine")
    public List<PayoutResponse> payoutsMine(@RequestHeader(HeaderNames.USER_ID) String userId) {
        return settlementService.payoutsMine(UUID.fromString(userId)).stream().map(PayoutResponse::from).toList();
    }
}
