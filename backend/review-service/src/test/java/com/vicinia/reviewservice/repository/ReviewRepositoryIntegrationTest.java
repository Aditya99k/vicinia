package com.vicinia.reviewservice.repository;

import com.vicinia.reviewservice.domain.Review;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DuplicateKeyException;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Stage 17 — proves Review's compound unique index on (userId, productId)
 * (Stage 13) is real, not just an annotation: a second review from the
 * same user for the same product fails at the database layer with
 * DuplicateKeyException, which ReviewService.create() catches and
 * translates to ReviewAlreadyExistsException — the actual race-safety net
 * behind the pre-check, proven here directly against a real Mongo engine
 * rather than assumed from Stage 13's live curl test alone.
 *
 * <p>Every (userId, productId) pair below is unique per test method — found
 * in CI that Mongo (unlike @DataJpaTest's Postgres, rolled back per test)
 * doesn't get any transactional rollback here, so documents persist across
 * test methods within the same class run; 4 tests all hardcoding
 * "user-1"/"product-1" meant whichever ran after another had already
 * inserted that pair hit a real DuplicateKeyException on its own unrelated
 * first save, regardless of execution order.
 */
@DataMongoTest
@Testcontainers
class ReviewRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    void aSecondReviewFromTheSameUserForTheSameProduct_violatesTheCompoundUniqueIndex() {
        String userId = "user-" + UUID.randomUUID();
        String productId = "product-" + UUID.randomUUID();
        reviewRepository.save(new Review(userId, productId, 5, "Great!"));

        assertThatThrownBy(() -> reviewRepository.save(new Review(userId, productId, 3, "Changed my mind")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void theSameUserCanReviewTwoDifferentProducts() {
        String userId = "user-" + UUID.randomUUID();
        reviewRepository.save(new Review(userId, "product-" + UUID.randomUUID(), 5, "Great!"));

        assertThatCodeDoesNotThrow(() -> reviewRepository.save(new Review(userId, "product-" + UUID.randomUUID(), 4, "Also good")));
    }

    @Test
    void twoDifferentUsersCanReviewTheSameProduct() {
        String productId = "product-" + UUID.randomUUID();
        reviewRepository.save(new Review("user-" + UUID.randomUUID(), productId, 5, "Great!"));

        assertThatCodeDoesNotThrow(() -> reviewRepository.save(new Review("user-" + UUID.randomUUID(), productId, 2, "Not for me")));
    }

    @Test
    void existsByUserIdAndProductId_findsAnExistingReview() {
        String userId = "user-" + UUID.randomUUID();
        String productId = "product-" + UUID.randomUUID();
        String otherProductId = "product-" + UUID.randomUUID();
        reviewRepository.save(new Review(userId, productId, 5, "Great!"));

        assertThat(reviewRepository.existsByUserIdAndProductId(userId, productId)).isTrue();
        assertThat(reviewRepository.existsByUserIdAndProductId(userId, otherProductId)).isFalse();
    }

    private static void assertThatCodeDoesNotThrow(Runnable action) {
        org.assertj.core.api.Assertions.assertThatCode(action::run).doesNotThrowAnyException();
    }
}
