package com.vicinia.settlementservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.settlementservice.dto.PayoutResponse;
import com.vicinia.settlementservice.dto.SettlementEntryResponse;
import com.vicinia.settlementservice.service.SettlementService;
import com.vicinia.settlementservice.util.PermissionUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Every endpoint here requires the SETTLEMENT_MANAGE permission (seeded onto ADMIN in auth-service's RoleSeeder), matching AdminMerchantController's exact pattern. */
@RestController
@RequestMapping("/api/settlements/admin")
public class AdminSettlementController {

    private static final String REQUIRED_PERMISSION = "SETTLEMENT_MANAGE";

    private final SettlementService settlementService;

    public AdminSettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping("/entries")
    public List<SettlementEntryResponse> entries(@RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return settlementService.allEntries().stream().map(SettlementEntryResponse::from).toList();
    }

    @GetMapping("/payouts")
    public List<PayoutResponse> payouts(@RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return settlementService.allPayouts().stream().map(PayoutResponse::from).toList();
    }

    /** On-demand run of the grouping batch job — real cadence is daily (see PayoutBatchJob), this is for ops/testing without waiting. */
    @PostMapping("/payouts/run-batch")
    public List<PayoutResponse> runBatch(@RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return settlementService.runBatch().stream().map(PayoutResponse::from).toList();
    }

    /** On-demand tick of the simulated payout processor — real cadence is PayoutProcessor's scheduled interval, this is for ops/testing without waiting. */
    @PostMapping("/payouts/run-processor")
    public void runProcessor(@RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        settlementService.runProcessorTick();
    }
}
