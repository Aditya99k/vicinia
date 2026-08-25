package com.vicinia.merchantservice.dto;

import com.vicinia.merchantservice.domain.Merchant;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

public record MerchantResponse(
        String id,
        String ownerUserId,
        String storeName,
        String description,
        String addressLine1,
        String city,
        String state,
        String pincode,
        Double latitude,
        Double longitude,
        Double deliveryRadiusKm,
        LocalTime openTime,
        LocalTime closeTime,
        String status,
        String rejectionReason,
        String suspensionReason,
        List<String> documentTypes,
        Instant createdAt,
        Instant updatedAt
) {
    public static MerchantResponse from(Merchant m) {
        return new MerchantResponse(
                m.getId().toString(),
                m.getOwnerUserId().toString(),
                m.getStoreName(),
                m.getDescription(),
                m.getAddressLine1(),
                m.getCity(),
                m.getState(),
                m.getPincode(),
                m.getLatitude(),
                m.getLongitude(),
                m.getDeliveryRadiusKm(),
                m.getOpenTime(),
                m.getCloseTime(),
                m.getStatus().name(),
                m.getRejectionReason(),
                m.getSuspensionReason(),
                m.getDocuments().stream().map(com.vicinia.merchantservice.domain.MerchantDocument::getDocumentType).toList(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }
}
