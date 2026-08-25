package com.vicinia.reviewservice.service;

import com.vicinia.reviewservice.client.OrderClient;
import com.vicinia.reviewservice.domain.Review;
import com.vicinia.reviewservice.dto.CreateReviewRequest;
import com.vicinia.reviewservice.dto.RatingAggregateResponse;
import com.vicinia.reviewservice.exception.ReviewAlreadyExistsException;
import com.vicinia.reviewservice.exception.ReviewNotEligibleException;
import com.vicinia.reviewservice.repository.ReviewRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderClient orderClient;
    private final MongoTemplate mongoTemplate;

    public ReviewService(ReviewRepository reviewRepository, OrderClient orderClient, MongoTemplate mongoTemplate) {
        this.reviewRepository = reviewRepository;
        this.orderClient = orderClient;
        this.mongoTemplate = mongoTemplate;
    }

    public Review create(String userId, CreateReviewRequest request) {
        if (!orderClient.hasDeliveredProduct(userId, request.productId())) {
            throw new ReviewNotEligibleException();
        }
        if (reviewRepository.existsByUserIdAndProductId(userId, request.productId())) {
            throw new ReviewAlreadyExistsException();
        }
        try {
            return reviewRepository.save(new Review(userId, request.productId(), request.rating(), request.comment()));
        } catch (DuplicateKeyException e) {
            // Closes the race window between the check above and this insert — the
            // compound unique index (userId, productId) is the real enforcement,
            // the pre-check above is just for a clean error on the common path.
            throw new ReviewAlreadyExistsException();
        }
    }

    public List<Review> forProduct(String productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    public List<Review> mine(String userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Computed on the fly rather than a stored running aggregate — read-heavy, low-write collection (ARCHITECTURE.md §6), so there's no drift to guard against by keeping it live. */
    public RatingAggregateResponse rating(String productId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("productId").is(productId)),
                Aggregation.group().avg("rating").as("averageRating").count().as("reviewCount")
        );
        AggregationResults<RatingAggregateResult> results = mongoTemplate.aggregate(aggregation, "reviews", RatingAggregateResult.class);
        RatingAggregateResult result = results.getUniqueMappedResult();
        if (result == null) {
            return new RatingAggregateResponse(productId, 0.0, 0);
        }
        return new RatingAggregateResponse(productId, result.averageRating(), result.reviewCount());
    }

    private record RatingAggregateResult(double averageRating, long reviewCount) {
    }
}
