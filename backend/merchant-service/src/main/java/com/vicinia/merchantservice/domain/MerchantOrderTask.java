package com.vicinia.merchantservice.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * merchant-service's own, minimal view of one order that needs the
 * merchant's action — created from order-service's order.confirmed event,
 * not owned by merchant-service. Pulled forward into Stage 11 specifically
 * so delivery-service has a real order.ready event to react to; see
 * BUILD_TRACKER.md's Stage 11 notes for why this isn't its own stage.
 */
@Entity
@Table(name = "merchant_order_tasks", uniqueConstraints = @UniqueConstraint(columnNames = "order_id"))
public class MerchantOrderTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID orderId;
    private UUID merchantId;

    @Enumerated(EnumType.STRING)
    private OrderTaskStatus status = OrderTaskStatus.PENDING_ACCEPTANCE;

    private String rejectionReason;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    protected MerchantOrderTask() {
    }

    public MerchantOrderTask(UUID orderId, UUID merchantId) {
        this.orderId = orderId;
        this.merchantId = merchantId;
    }

    public void transitionTo(OrderTaskStatus target) {
        OrderTaskStatusTransition.assertAllowed(this.status, target);
        this.status = target;
        this.updatedAt = Instant.now();
    }

    public void reject(String reason) {
        transitionTo(OrderTaskStatus.REJECTED);
        this.rejectionReason = reason;
    }

    // --- getters ---

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public OrderTaskStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
