package com.vicinia.reviewservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.reviewservice.dto.CreateReviewRequest;
import com.vicinia.reviewservice.dto.RatingAggregateResponse;
import com.vicinia.reviewservice.dto.ReviewResponse;
import com.vicinia.reviewservice.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * POST/mine require auth (X-User-Id) — creating or listing "my own"
 * reviews is self-scoped, same as order-service's /mine, with no separate
 * RBAC permission (any authenticated role may review a product they
 * actually received). GET /products/** is public (see api-gateway's
 * public-paths) — browsing reviews needs no login, matching catalog's own
 * public search/browse endpoints.
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> create(@RequestHeader(HeaderNames.USER_ID) String userId,
                                                  @Valid @RequestBody CreateReviewRequest request) {
        var review = reviewService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReviewResponse.from(review));
    }

    @GetMapping("/mine")
    public List<ReviewResponse> mine(@RequestHeader(HeaderNames.USER_ID) String userId) {
        return reviewService.mine(userId).stream().map(ReviewResponse::from).toList();
    }

    @GetMapping("/products/{productId}")
    public List<ReviewResponse> forProduct(@PathVariable String productId) {
        return reviewService.forProduct(productId).stream().map(ReviewResponse::from).toList();
    }

    @GetMapping("/products/{productId}/rating")
    public RatingAggregateResponse rating(@PathVariable String productId) {
        return reviewService.rating(productId);
    }
}
