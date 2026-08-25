package com.vicinia.reviewservice.dto;

import com.vicinia.reviewservice.domain.Review;

import java.time.Instant;

public record ReviewResponse(
        String id,
        String userId,
        String productId,
        int rating,
        String comment,
        Instant createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getUserId(),
                review.getProductId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
