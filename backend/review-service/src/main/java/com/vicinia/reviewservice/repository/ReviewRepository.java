package com.vicinia.reviewservice.repository;

import com.vicinia.reviewservice.domain.Review;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {

    List<Review> findByProductIdOrderByCreatedAtDesc(String productId);

    List<Review> findByUserIdOrderByCreatedAtDesc(String userId);

    boolean existsByUserIdAndProductId(String userId, String productId);
}
