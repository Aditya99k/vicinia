package com.vicinia.catalogservice.repository;

import com.vicinia.catalogservice.domain.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Stage 17 — proves Category's @Indexed(unique = true) on name/slug is
 * actually enforced by the real database, not just present as an
 * annotation. Found and fixed a real bug while writing this: catalog-
 * service never enabled spring.data.mongodb.auto-index-creation (off by
 * default), so this index has never actually existed since Stage 4 —
 * review-service got the same fix in Stage 13, catalog-service was never
 * revisited until this test exercised it. See that application.yml
 * change for the full story.
 */
@DataMongoTest
@Testcontainers
class CategoryRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void savingTwoCategoriesWithTheSameName_violatesTheUniqueIndex() {
        categoryRepository.save(new Category("Dairy & Eggs", "dairy-eggs"));

        assertThatThrownBy(() -> categoryRepository.save(new Category("Dairy & Eggs", "dairy-eggs-2")))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @Test
    void savingTwoCategoriesWithTheSameSlug_violatesTheUniqueIndex() {
        categoryRepository.save(new Category("Snacks", "snacks"));

        assertThatThrownBy(() -> categoryRepository.save(new Category("Snacks & Chips", "snacks")))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @Test
    void existsByNameIgnoreCase_isCaseInsensitive() {
        categoryRepository.save(new Category("Beverages", "beverages"));

        assertThat(categoryRepository.existsByNameIgnoreCase("beverages")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("BEVERAGES")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("Snacks")).isFalse();
    }
}
