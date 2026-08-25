package com.vicinia.reviewservice.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One review per (userId, productId) — enforced by a real unique compound
 * index, not a check-then-insert in the service layer, which would leave a
 * race window between the existence check and the save. A user can review
 * a product once, however many times they've bought it; BUILD_TRACKER.md's
 * own "done when" only requires the delivered-order gate, not a review per
 * purchase.
 */
@Document(collection = "reviews")
@CompoundIndex(def = "{'userId': 1, 'productId': 1}", unique = true)
public class Review {

    @Id
    private String id;

    private String userId;
    private String productId;
    private int rating;
    private String comment;

    private Instant createdAt = Instant.now();

    protected Review() {
    }

    public Review(String userId, String productId, int rating, String comment) {
        this.userId = userId;
        this.productId = productId;
        this.rating = rating;
        this.comment = comment;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getProductId() {
        return productId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
