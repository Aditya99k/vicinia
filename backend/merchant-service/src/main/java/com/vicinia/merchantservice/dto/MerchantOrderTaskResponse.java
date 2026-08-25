package com.vicinia.merchantservice.dto;

import com.vicinia.merchantservice.domain.MerchantOrderTask;
import com.vicinia.merchantservice.domain.OrderTaskStatus;

import java.time.Instant;
import java.util.UUID;

public record MerchantOrderTaskResponse(
        UUID orderId,
        UUID merchantId,
        OrderTaskStatus status,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static MerchantOrderTaskResponse from(MerchantOrderTask task) {
        return new MerchantOrderTaskResponse(
                task.getOrderId(), task.getMerchantId(), task.getStatus(),
                task.getRejectionReason(), task.getCreatedAt(), task.getUpdatedAt()
        );
    }
}
