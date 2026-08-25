package com.vicinia.settlementservice.dto;

import com.vicinia.settlementservice.domain.Payout;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayoutResponse(
        UUID id,
        UUID merchantId,
        BigDecimal totalAmount,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static PayoutResponse from(Payout payout) {
        return new PayoutResponse(
                payout.getId(), payout.getMerchantId(), payout.getTotalAmount(),
                payout.getStatus().name(), payout.getCreatedAt(), payout.getUpdatedAt()
        );
    }
}
