package com.vicinia.merchantservice.dto;

import com.vicinia.merchantservice.domain.Merchant;

import java.time.LocalTime;

/** Public-facing shape for GET /api/merchants/nearby — no owner/document/status internals. */
public record MerchantSummaryResponse(
        String id,
        String storeName,
        String description,
        String city,
        Double deliveryRadiusKm,
        LocalTime openTime,
        LocalTime closeTime
) {
    public static MerchantSummaryResponse from(Merchant m) {
        return new MerchantSummaryResponse(
                m.getId().toString(),
                m.getStoreName(),
                m.getDescription(),
                m.getCity(),
                m.getDeliveryRadiusKm(),
                m.getOpenTime(),
                m.getCloseTime()
        );
    }
}
