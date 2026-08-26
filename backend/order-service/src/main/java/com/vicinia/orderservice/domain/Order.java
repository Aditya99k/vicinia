package com.vicinia.orderservice.domain;

import com.vicinia.orderservice.dto.PaymentMethod;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * order-service owns the single canonical status field for the entire
 * lifecycle (ADR 0004) — even the states this service itself never drives
 * yet (MERCHANT_ACCEPTED onward), so later stages mirror status here via
 * Kafka rather than composing live reads across three services.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;
    private UUID merchantId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    private String couponCode;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    private String cancellationReason;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    /** True once real money has actually moved — WALLET/RAZORPAY as soon as that payment succeeds, never for COD (collected in person by the delivery partner, outside anything this system tracks). */
    private boolean paid = false;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public Order(UUID userId, UUID merchantId, BigDecimal subtotal, PaymentMethod paymentMethod) {
        this.userId = userId;
        this.merchantId = merchantId;
        this.subtotal = subtotal;
        this.totalAmount = subtotal;
        this.paymentMethod = paymentMethod;
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void transitionTo(OrderStatus target) {
        OrderStatusTransition.assertAllowed(this.status, target);
        this.status = target;
        this.updatedAt = Instant.now();
    }

    public void applyCoupon(String couponCode, BigDecimal discountAmount) {
        this.couponCode = couponCode;
        this.discountAmount = discountAmount;
        this.totalAmount = subtotal.subtract(discountAmount);
        this.updatedAt = Instant.now();
    }

    public void setCancellationReason(String reason) {
        this.cancellationReason = reason;
        this.updatedAt = Instant.now();
    }

    // --- getters ---

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public boolean isPaid() {
        return paid;
    }

    public void markPaid() {
        this.paid = true;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
