package com.vicinia.couponservice.dto;

import com.vicinia.couponservice.domain.CouponUsage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApplyCouponResponse(
        UUID couponId,
        UUID orderId,
        BigDecimal discountAmount,
        Instant usedAt
) {
    public static ApplyCouponResponse from(CouponUsage usage) {
        return new ApplyCouponResponse(usage.getCouponId(), usage.getOrderId(), usage.getDiscountAmount(), usage.getUsedAt());
    }
}
