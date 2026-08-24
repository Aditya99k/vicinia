package com.vicinia.inventoryservice.domain;

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
 * One row per (merchant, product) pair — the per-merchant price/stock fact
 * that catalog-service's Product deliberately does not carry (ARCHITECTURE.md
 * §4.2, ADR 0007). merchantId correlates by value to merchant-service's
 * Merchant.ownerUserId (the merchant's own auth user id, i.e. the caller's
 * X-User-Id), not a foreign key — the same cross-database correlation
 * pattern user-service and merchant-service already use, and the only way
 * to scope "my listings" without an unlisted synchronous dependency on
 * merchant-service (ARCHITECTURE.md §7's table names catalog-service as
 * inventory-service's only REST dependency). productId correlates by value
 * to catalog-service's Product.id (a Mongo ObjectId string).
 *
 * productName/productCategory are a snapshot taken at listing-creation time
 * (via the local KnownProduct cache, falling back to a direct catalog-service
 * REST call — see CatalogClient) so listing reads never need a live call
 * back to catalog-service.
 */
@Entity
@Table(name = "merchant_listings", uniqueConstraints = @UniqueConstraint(columnNames = {"merchant_id", "product_id"}))
public class MerchantListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID merchantId;
    private String productId;
    private String productName;
    private String productCategory;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private int availableStock;
    private int reservedStock = 0;
    private boolean active = true;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    protected MerchantListing() {
    }

    public MerchantListing(UUID merchantId, String productId, String productName, String productCategory,
                            BigDecimal price, int availableStock) {
        this.merchantId = merchantId;
        this.productId = productId;
        this.productName = productName;
        this.productCategory = productCategory;
        this.price = price;
        this.availableStock = availableStock;
    }

    public void updateDetails(BigDecimal price, Integer availableStock, Boolean active) {
        if (price != null) {
            this.price = price;
        }
        if (availableStock != null) {
            this.availableStock = availableStock;
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

    public UUID getMerchantId() {
        return merchantId;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public int getReservedStock() {
        return reservedStock;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
