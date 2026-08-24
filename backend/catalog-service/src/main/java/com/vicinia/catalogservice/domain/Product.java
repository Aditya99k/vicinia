package com.vicinia.catalogservice.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The global, canonical product (ARCHITECTURE.md §4.2) — "what is this
 * item", shared across every merchant that sells it. Deliberately has no
 * merchantId: price/stock/who-sells-it belongs to inventory-service's
 * MerchantListing (Stage 5), not here. MongoDB fits because attributes{}
 * varies wildly per category (a phone's attributes look nothing like a
 * fruit's) and reads vastly outnumber writes once a product exists.
 */
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    private String name;
    private String brand;
    private String category;
    private String description;
    private List<String> images;
    private Map<String, Object> attributes;

    private ProductStatus status;
    private String requestedByUserId;
    private String rejectionReason;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    protected Product() {
    }

    public Product(String name, String brand, String category, String description,
                    List<String> images, Map<String, Object> attributes,
                    ProductStatus status, String requestedByUserId) {
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.description = description;
        this.images = images;
        this.attributes = attributes;
        this.status = status;
        this.requestedByUserId = requestedByUserId;
    }

    public void approve() {
        this.status = ProductStatus.APPROVED;
        this.updatedAt = Instant.now();
    }

    public void reject(String reason) {
        this.status = ProductStatus.REJECTED;
        this.rejectionReason = reason;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getImages() {
        return images;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public String getRequestedByUserId() {
        return requestedByUserId;
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
