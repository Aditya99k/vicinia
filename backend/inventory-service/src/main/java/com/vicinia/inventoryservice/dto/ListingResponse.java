package com.vicinia.inventoryservice.dto;

import com.vicinia.inventoryservice.domain.MerchantListing;

import java.math.BigDecimal;
import java.util.UUID;

public record ListingResponse(
        UUID id,
        UUID merchantId,
        String productId,
        String productName,
        String productCategory,
        BigDecimal price,
        int availableStock,
        int reservedStock,
        boolean active
) {
    public static ListingResponse from(MerchantListing listing) {
        return new ListingResponse(
                listing.getId(), listing.getMerchantId(), listing.getProductId(),
                listing.getProductName(), listing.getProductCategory(), listing.getPrice(),
                listing.getAvailableStock(), listing.getReservedStock(), listing.isActive()
        );
    }
}
