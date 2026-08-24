package com.vicinia.paymentservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One row per internal order that pays via Razorpay, created in CREATED
 * state at order-creation time and resolved to SUCCESS/FAILED when the
 * webhook arrives. razorpayPaymentId has its own unique constraint per
 * ARCHITECTURE.md §4.6/§11 — a genuine DB-level backstop against the same
 * razorpay_payment_id ever attaching to two different orders — but the
 * primary idempotency mechanism is the status-check-before-transition
 * pattern in RazorpayPaymentService, the same one ADR 0004/§4.6 already
 * describes for order-service's own Kafka consumption ("idempotent on
 * orderId + target status").
 */
@Entity
@Table(name = "razorpay_payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_razorpay_order_id", columnNames = "order_id"),
                @UniqueConstraint(name = "uk_razorpay_payment_id", columnNames = "razorpay_payment_id")
        })
public class RazorpayPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID orderId;
    private UUID userId;
    private String razorpayOrderId;
    private String razorpayPaymentId;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private RazorpayPaymentStatus status = RazorpayPaymentStatus.CREATED;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    protected RazorpayPayment() {
    }

    public RazorpayPayment(UUID orderId, UUID userId, String razorpayOrderId, BigDecimal amount) {
        this.orderId = orderId;
        this.userId = userId;
        this.razorpayOrderId = razorpayOrderId;
        this.amount = amount;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public RazorpayPaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
