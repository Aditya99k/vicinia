package com.vicinia.settlementservice.domain;

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
 * One row per delivered order (ARCHITECTURE.md §4.7) — gross is what the
 * customer actually paid (order.totalAmount, post-discount), commission
 * and net are computed at creation time from the platform's commission
 * rate, not recomputed later, so a future rate change never rewrites
 * history for orders already settled.
 */
@Entity
@Table(name = "settlement_entries", uniqueConstraints = @UniqueConstraint(columnNames = "order_id"))
public class SettlementEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID orderId;
    private UUID merchantId;

    @Column(precision = 10, scale = 2)
    private BigDecimal gross;

    @Column(precision = 10, scale = 2)
    private BigDecimal commission;

    @Column(precision = 10, scale = 2)
    private BigDecimal net;

    @Enumerated(EnumType.STRING)
    private SettlementEntryStatus status = SettlementEntryStatus.PENDING;

    private UUID payoutId;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    protected SettlementEntry() {
    }

    public SettlementEntry(UUID orderId, UUID merchantId, BigDecimal gross, BigDecimal commission, BigDecimal net) {
        this.orderId = orderId;
        this.merchantId = merchantId;
        this.gross = gross;
        this.commission = commission;
        this.net = net;
    }

    public void assignToPayout(UUID payoutId) {
        this.payoutId = payoutId;
        this.status = SettlementEntryStatus.SETTLED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public BigDecimal getGross() {
        return gross;
    }

    public BigDecimal getCommission() {
        return commission;
    }

    public BigDecimal getNet() {
        return net;
    }

    public SettlementEntryStatus getStatus() {
        return status;
    }

    public UUID getPayoutId() {
        return payoutId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
