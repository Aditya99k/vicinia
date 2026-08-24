package com.vicinia.couponservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.couponservice.domain.CouponUsage;
import com.vicinia.couponservice.dto.ApplyCouponRequest;
import com.vicinia.couponservice.dto.ApplyCouponResponse;
import com.vicinia.couponservice.dto.ValidateCouponResponse;
import com.vicinia.couponservice.service.CouponService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

/** Any authenticated user checking/using a coupon on their own order — no special permission beyond a valid JWT. */
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/validate")
    public ValidateCouponResponse validate(@RequestHeader(HeaderNames.USER_ID) String userId,
                                            @RequestParam @NotBlank String code,
                                            @RequestParam BigDecimal orderValue) {
        CouponService.PreviewResult preview = couponService.validate(code, UUID.fromString(userId), orderValue);
        return new ValidateCouponResponse(
                preview.coupon().getCode(), preview.coupon().getDiscountType(),
                preview.coupon().getDiscountValue(), preview.discountAmount(), preview.coupon().getMinOrderValue());
    }

    @PostMapping("/apply")
    public ResponseEntity<ApplyCouponResponse> apply(@RequestHeader(HeaderNames.USER_ID) String userId,
                                                       @Valid @RequestBody ApplyCouponRequest request) {
        CouponUsage usage = couponService.apply(UUID.fromString(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApplyCouponResponse.from(usage));
    }
}
