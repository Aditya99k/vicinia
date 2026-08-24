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
 * The transaction ledger (minimal slice — see pom.xml's module description).
 * orderId is null for a TOPUP (not tied to any order); DEBIT and CREDIT
 * always carry one. The unique (order_id, type) constraint — not just
 * order_id — is what makes pay() and refund() each independently
 * idempotent while still letting a paid order later carry both a DEBIT and
 * its eventual refund CREDIT under the same orderId.
 */
@Entity
@Table(name = "wallet_transactions", uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "type"}))
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    private Instant createdAt = Instant.now();

    protected WalletTransaction() {
    }

    public WalletTransaction(UUID userId, UUID orderId, TransactionType type, BigDecimal amount) {
        this.userId = userId;
        this.orderId = orderId;
        this.type = type;
        this.amount = amount;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
