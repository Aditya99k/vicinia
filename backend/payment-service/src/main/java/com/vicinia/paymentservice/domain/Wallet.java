package com.vicinia.paymentservice.domain;

import jakarta.persistence.Column;
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
 * One row per user, auto-provisioned by WalletProvisionConsumer on
 * user.registered (mirroring user-service's own UserProfile auto-creation
 * from Stage 2) rather than requiring a separate "create my wallet" step.
 * Balance mutations always go through the atomic conditional UPDATEs in
 * WalletRepository (tryDebit/credit) — ADR 0002's pattern, same family as
 * inventory and coupons.
 */
@Entity
@Table(name = "wallets", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;

    @Column(precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    protected Wallet() {
    }

    public Wallet(UUID userId) {
        this.userId = userId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
