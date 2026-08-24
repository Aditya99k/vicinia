package com.vicinia.couponservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.couponservice.domain.Coupon;
import com.vicinia.couponservice.dto.CouponResponse;
import com.vicinia.couponservice.dto.CreateCouponRequest;
import com.vicinia.couponservice.dto.UpdateCouponRequest;
import com.vicinia.couponservice.service.CouponService;
import com.vicinia.couponservice.util.PermissionUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Gated by COUPON_MANAGE — already seeded onto ADMIN in auth-service's RoleSeeder since Stage 2. */
@RestController
@RequestMapping("/api/coupons/admin")
public class AdminCouponController {

    private static final String REQUIRED_PERMISSION = "COUPON_MANAGE";

    private final CouponService couponService;

    public AdminCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    public ResponseEntity<CouponResponse> create(@RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions,
                                                  @Valid @RequestBody CreateCouponRequest request) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        Coupon coupon = couponService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CouponResponse.from(coupon));
    }

    @GetMapping
    public List<CouponResponse> list(@RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return couponService.listAll().stream().map(CouponResponse::from).toList();
    }

    @PutMapping("/{id}")
    public CouponResponse update(@RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody UpdateCouponRequest request) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return CouponResponse.from(couponService.update(id, request));
    }
}
