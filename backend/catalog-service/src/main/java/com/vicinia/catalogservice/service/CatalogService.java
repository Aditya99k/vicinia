package com.vicinia.catalogservice.service;

import com.vicinia.catalogservice.domain.Category;
import com.vicinia.catalogservice.domain.Product;
import com.vicinia.catalogservice.domain.ProductStatus;
import com.vicinia.catalogservice.dto.CategoryRequest;
import com.vicinia.catalogservice.dto.ProductRequest;
import com.vicinia.catalogservice.exception.CategoryAlreadyExistsException;
import com.vicinia.catalogservice.exception.CategoryNotFoundException;
import com.vicinia.catalogservice.exception.IllegalProductStatusException;
import com.vicinia.catalogservice.exception.ProductNotFoundException;
import com.vicinia.catalogservice.messaging.ProductEventPublisher;
import com.vicinia.catalogservice.repository.CategoryRepository;
import com.vicinia.catalogservice.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductEventPublisher eventPublisher;

    public CatalogService(ProductRepository productRepository,
                           CategoryRepository categoryRepository,
                           ProductEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.eventPublisher = eventPublisher;
    }

    public Product createByAdmin(ProductRequest request) {
        assertCategoryExists(request.category());
        Product product = new Product(
                request.name(), request.brand(), request.category(), request.description(),
                request.images(), request.attributes(), ProductStatus.APPROVED, null
        );
        product = productRepository.save(product);
        eventPublisher.publishCreated(product.getId());
        return product;
    }

    public Product requestCreation(ProductRequest request, String requestedByUserId) {
        assertCategoryExists(request.category());
        Product product = new Product(
                request.name(), request.brand(), request.category(), request.description(),
                request.images(), request.attributes(), ProductStatus.PENDING_REVIEW, requestedByUserId
        );
        return productRepository.save(product);
    }

    public Product getById(String id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    public List<Product> myRequests(String userId) {
        return productRepository.findByRequestedByUserId(userId);
    }

    public List<Product> search(String query, String category) {
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasCategory = category != null && !category.isBlank();

        if (hasQuery && hasCategory) {
            return productRepository.findByStatusAndNameContainingIgnoreCaseAndCategoryIgnoreCase(
                    ProductStatus.APPROVED, query, category);
        }
        if (hasQuery) {
            return productRepository.findByStatusAndNameContainingIgnoreCase(ProductStatus.APPROVED, query);
        }
        if (hasCategory) {
            return productRepository.findByStatusAndCategoryIgnoreCase(ProductStatus.APPROVED, category);
        }
        return productRepository.findByStatusOrderByCreatedAtAsc(ProductStatus.APPROVED);
    }

    public List<Category> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    // --- admin ---

    public List<Product> pendingReview() {
        return productRepository.findByStatusOrderByCreatedAtAsc(ProductStatus.PENDING_REVIEW);
    }

    public Product approve(String id) {
        Product product = getById(id);
        if (product.getStatus() != ProductStatus.PENDING_REVIEW) {
            throw new IllegalProductStatusException(
                    "Only a PENDING_REVIEW product can be approved (this one is " + product.getStatus() + ")");
        }
        product.approve();
        product = productRepository.save(product);
        eventPublisher.publishCreated(product.getId());
        return product;
    }

    public Product reject(String id, String reason) {
        Product product = getById(id);
        if (product.getStatus() != ProductStatus.PENDING_REVIEW) {
            throw new IllegalProductStatusException(
                    "Only a PENDING_REVIEW product can be rejected (this one is " + product.getStatus() + ")");
        }
        product.reject(reason);
        return productRepository.save(product);
    }

    public Category createCategory(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new CategoryAlreadyExistsException(request.name());
        }
        String slug = request.name().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return categoryRepository.save(new Category(request.name(), slug));
    }

    private void assertCategoryExists(String category) {
        if (categoryRepository.findByNameIgnoreCase(category).isEmpty()) {
            throw new CategoryNotFoundException(category);
        }
    }
}
