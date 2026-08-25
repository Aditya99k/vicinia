package com.vicinia.reviewservice.dto;

public record RatingAggregateResponse(String productId, double averageRating, long reviewCount) {
}
