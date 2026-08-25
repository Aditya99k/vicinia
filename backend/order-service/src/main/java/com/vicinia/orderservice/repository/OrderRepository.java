package com.vicinia.orderservice.repository;

import com.vicinia.orderservice.domain.Order;
import com.vicinia.orderservice.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** review-service's eligibility check (Stage 13): has this user received a delivered order containing this product. */
    boolean existsByUserIdAndStatusAndItems_ProductId(UUID userId, OrderStatus status, String productId);
}
