package com.vicinia.merchantservice.dto;

import com.vicinia.merchantservice.domain.Merchant;

import java.time.LocalTime;

/**
 * Public-facing shape for GET /api/merchants/nearby — no document/status
 * internals. ownerUserId IS included despite being an internal identity:
 * it's the same value inventory-service already returns as ListingResponse
 * .merchantId / CartResponse.merchantId to any authenticated customer
 * browsing a product's offers, so withholding it here wouldn't protect
 * anything, just stop the frontend from being able to join "this store" to
 * "its listings" (no listings-by-merchant endpoint exists — see
 * frontend/customer-app/README.md).
 */
public record MerchantSummaryResponse(
        String id,
        String ownerUserId,
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
                m.getOwnerUserId().toString(),
                m.getStoreName(),
                m.getDescription(),
                m.getCity(),
                m.getDeliveryRadiusKm(),
                m.getOpenTime(),
                m.getCloseTime()
        );
    }
}
