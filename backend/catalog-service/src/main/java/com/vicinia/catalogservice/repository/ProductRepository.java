package com.vicinia.catalogservice.repository;

import com.vicinia.catalogservice.domain.Product;
import com.vicinia.catalogservice.domain.ProductStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findByStatusOrderByCreatedAtAsc(ProductStatus status);

    List<Product> findByStatusAndNameContainingIgnoreCase(ProductStatus status, String name);

    List<Product> findByStatusAndCategoryIgnoreCase(ProductStatus status, String category);

    List<Product> findByStatusAndNameContainingIgnoreCaseAndCategoryIgnoreCase(
            ProductStatus status, String name, String category);

    List<Product> findByRequestedByUserId(String userId);
}
