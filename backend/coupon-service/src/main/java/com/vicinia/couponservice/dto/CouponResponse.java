package com.vicinia.couponservice.dto;

import com.vicinia.couponservice.domain.Coupon;
import com.vicinia.couponservice.domain.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponResponse(
        UUID id,
        String code,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minOrderValue,
        Integer usageLimit,
        int usageCount,
        Integer perUserLimit,
        boolean active,
        Instant validFrom,
        Instant validUntil
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(), coupon.getCode(), coupon.getDescription(), coupon.getDiscountType(),
                coupon.getDiscountValue(), coupon.getMaxDiscountAmount(), coupon.getMinOrderValue(),
                coupon.getUsageLimit(), coupon.getUsageCount(), coupon.getPerUserLimit(),
                coupon.isActive(), coupon.getValidFrom(), coupon.getValidUntil()
        );
    }
}
