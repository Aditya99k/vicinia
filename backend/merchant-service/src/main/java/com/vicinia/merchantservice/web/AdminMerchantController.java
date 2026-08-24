package com.vicinia.merchantservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.merchantservice.dto.MerchantResponse;
import com.vicinia.merchantservice.dto.ReasonRequest;
import com.vicinia.merchantservice.service.MerchantService;
import com.vicinia.merchantservice.util.PermissionUtil;
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

/**
 * Every endpoint here requires the MERCHANT_APPROVE permission (seeded onto
 * ADMIN in auth-service's RoleSeeder — see docs/ARCHITECTURE.md §2/§14).
 * The gateway already guarantees the caller is authenticated; this is the
 * second, finer-grained layer checking they're specifically allowed to
 * approve merchants.
 */
@RestController
@RequestMapping("/api/merchants/admin")
public class AdminMerchantController {

    private static final String REQUIRED_PERMISSION = "MERCHANT_APPROVE";

    private final MerchantService merchantService;

    public AdminMerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @GetMapping("/pending")
    public List<MerchantResponse> pending(@RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return merchantService.pendingReview().stream().map(MerchantResponse::from).toList();
    }

    @PostMapping("/{id}/approve")
    public MerchantResponse approve(@RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions,
                                     @PathVariable String id) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return MerchantResponse.from(merchantService.approve(UUID.fromString(id)));
    }

    @PostMapping("/{id}/reject")
    public MerchantResponse reject(@RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions,
                                    @PathVariable String id,
                                    @Valid @RequestBody ReasonRequest request) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return MerchantResponse.from(merchantService.reject(UUID.fromString(id), request.reason()));
    }

    @PostMapping("/{id}/suspend")
    public MerchantResponse suspend(@RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions,
                                     @PathVariable String id,
                                     @Valid @RequestBody ReasonRequest request) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return MerchantResponse.from(merchantService.suspend(UUID.fromString(id), request.reason()));
    }

    @PostMapping("/{id}/reinstate")
    public MerchantResponse reinstate(@RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions,
                                       @PathVariable String id) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return MerchantResponse.from(merchantService.reinstate(UUID.fromString(id)));
    }
}
