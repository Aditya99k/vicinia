package com.vicinia.settlementservice.dto;

import com.vicinia.settlementservice.domain.SettlementEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SettlementEntryResponse(
        UUID id,
        UUID orderId,
        UUID merchantId,
        BigDecimal gross,
        BigDecimal commission,
        BigDecimal net,
        String status,
        UUID payoutId,
        Instant createdAt
) {
    public static SettlementEntryResponse from(SettlementEntry entry) {
        return new SettlementEntryResponse(
                entry.getId(), entry.getOrderId(), entry.getMerchantId(),
                entry.getGross(), entry.getCommission(), entry.getNet(),
                entry.getStatus().name(), entry.getPayoutId(), entry.getCreatedAt()
        );
    }
}
