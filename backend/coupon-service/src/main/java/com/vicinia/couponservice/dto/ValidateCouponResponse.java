package com.vicinia.couponservice.dto;

import com.vicinia.couponservice.domain.DiscountType;

import java.math.BigDecimal;

public record ValidateCouponResponse(
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal discountAmount,
        BigDecimal minOrderValue
) {
}
