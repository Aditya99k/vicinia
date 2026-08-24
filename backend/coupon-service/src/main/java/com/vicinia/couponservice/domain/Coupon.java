package com.vicinia.couponservice.domain;

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
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * usageCount/usageLimit is the field pair ARCHITECTURE.md §6 calls "the
 * same family as inventory" — a global cap enforced by the exact same
 * atomic-conditional-UPDATE pattern as ADR 0002's reservation stock
 * (CouponRepository.tryIncrementUsage), not by reading usageCount here and
 * deciding in application code. usageLimit/perUserLimit null means
 * unlimited on that axis.
 */
@Entity
@Table(name = "coupons", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String code;
    private String description;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    private Integer usageLimit;
    private int usageCount = 0;
    private Integer perUserLimit;

    private boolean active = true;
    private Instant validFrom;
    private Instant validUntil;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    protected Coupon() {
    }

    public Coupon(String code, String description, DiscountType discountType, BigDecimal discountValue,
                  BigDecimal maxDiscountAmount, BigDecimal minOrderValue, Integer usageLimit,
                  Integer perUserLimit, Instant validFrom, Instant validUntil) {
        this.code = code;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        if (minOrderValue != null) {
            this.minOrderValue = minOrderValue;
        }
        this.usageLimit = usageLimit;
        this.perUserLimit = perUserLimit;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    public boolean isCurrentlyValid(Instant now) {
        if (!active) {
            return false;
        }
        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }
        return validUntil == null || !now.isAfter(validUntil);
    }

    /** Percentage/flat, capped by maxDiscountAmount if set, and never more than the order itself is worth. */
    public BigDecimal computeDiscount(BigDecimal orderValue) {
        BigDecimal raw = discountType == DiscountType.PERCENTAGE
                ? orderValue.multiply(discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : discountValue;

        if (maxDiscountAmount != null && raw.compareTo(maxDiscountAmount) > 0) {
            raw = maxDiscountAmount;
        }
        if (raw.compareTo(orderValue) > 0) {
            raw = orderValue;
        }
        return raw;
    }

    public void applyUpdate(String description, BigDecimal discountValue, BigDecimal maxDiscountAmount,
                             BigDecimal minOrderValue, Integer usageLimit, Integer perUserLimit,
                             Instant validFrom, Instant validUntil, Boolean active) {
        if (description != null) {
            this.description = description;
        }
        if (discountValue != null) {
            this.discountValue = discountValue;
        }
        if (maxDiscountAmount != null) {
            this.maxDiscountAmount = maxDiscountAmount;
        }
        if (minOrderValue != null) {
            this.minOrderValue = minOrderValue;
        }
        if (usageLimit != null) {
            this.usageLimit = usageLimit;
        }
        if (perUserLimit != null) {
            this.perUserLimit = perUserLimit;
        }
        if (validFrom != null) {
            this.validFrom = validFrom;
        }
        if (validUntil != null) {
            this.validUntil = validUntil;
        }
        if (active != null) {
            this.active = active;
        }
        this.updatedAt = Instant.now();
    }

    // --- getters ---

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public BigDecimal getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public BigDecimal getMinOrderValue() {
        return minOrderValue;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public Integer getPerUserLimit() {
        return perUserLimit;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
