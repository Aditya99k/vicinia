package com.vicinia.deliveryservice.dto;

import com.vicinia.deliveryservice.domain.DeliveryPartner;
import com.vicinia.deliveryservice.domain.PartnerStatus;

import java.util.UUID;

public record PartnerResponse(UUID userId, PartnerStatus status) {
    public static PartnerResponse from(DeliveryPartner partner) {
        return new PartnerResponse(partner.getUserId(), partner.getStatus());
    }
}
