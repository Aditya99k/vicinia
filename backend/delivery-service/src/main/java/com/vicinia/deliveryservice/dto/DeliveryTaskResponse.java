package com.vicinia.deliveryservice.dto;

import com.vicinia.deliveryservice.domain.DeliveryTask;
import com.vicinia.deliveryservice.domain.DeliveryTaskStatus;

import java.time.Instant;
import java.util.UUID;

public record DeliveryTaskResponse(
        UUID orderId,
        UUID merchantId,
        UUID partnerId,
        DeliveryTaskStatus status,
        Instant assignedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static DeliveryTaskResponse from(DeliveryTask task) {
        return new DeliveryTaskResponse(
                task.getOrderId(), task.getMerchantId(), task.getPartnerId(), task.getStatus(),
                task.getAssignedAt(), task.getCreatedAt(), task.getUpdatedAt()
        );
    }
}
