package com.vicinia.paymentservice.dto;

import com.vicinia.paymentservice.domain.TransactionType;
import com.vicinia.paymentservice.domain.WalletTransaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID userId,
        UUID orderId,
        TransactionType type,
        BigDecimal amount,
        Instant createdAt
) {
    public static TransactionResponse from(WalletTransaction transaction) {
        return new TransactionResponse(
                transaction.getId(), transaction.getUserId(), transaction.getOrderId(),
                transaction.getType(), transaction.getAmount(), transaction.getCreatedAt()
        );
    }
}
