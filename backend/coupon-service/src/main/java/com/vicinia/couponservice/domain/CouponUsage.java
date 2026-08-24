package com.vicinia.couponservice.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One row per successful apply — the unique (coupon_id, order_id)
 * constraint is what makes apply idempotent (same shape as Stage 5's
 * Reservation on (order_id, listing_id)): re-applying the same coupon to
 * the same order returns the existing row rather than counting twice.
 */
@Entity
@Table(name = "coupon_usages", uniqueConstraints = @UniqueConstraint(columnNames = {"coupon_id", "order_id"}))
public class CouponUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID couponId;
    private UUID userId;
    private UUID orderId;

    private BigDecimal discountAmount;
    private Instant usedAt = Instant.now();

    protected CouponUsage() {
    }

    public CouponUsage(UUID couponId, UUID userId, UUID orderId, BigDecimal discountAmount) {
        this.couponId = couponId;
        this.userId = userId;
        this.orderId = orderId;
        this.discountAmount = discountAmount;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCouponId() {
        return couponId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
