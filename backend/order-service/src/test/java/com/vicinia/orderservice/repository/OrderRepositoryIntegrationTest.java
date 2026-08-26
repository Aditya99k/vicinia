package com.vicinia.orderservice.repository;

import com.vicinia.orderservice.domain.Order;
import com.vicinia.orderservice.domain.OrderItem;
import com.vicinia.orderservice.domain.OrderStatus;
import com.vicinia.orderservice.dto.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stage 17 — order creation/status persistence against a real Postgres,
 * plus the two derived queries added in later stages that traverse the
 * Order -> OrderItem relation or filter on a timestamp column:
 * existsByUserIdAndStatusAndItems_ProductId (review-service's Stage 13
 * eligibility gate) and countByStatusAndUpdatedAtBefore (Stage 16's stale-
 * order gauge) — both are exactly the kind of derived-query correctness
 * an embedded H2 database can mask if its SQL dialect quietly diverges
 * from Postgres's.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OrderRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OrderRepository orderRepository;

    private Order newOrder(UUID userId, String productId, OrderStatus status) {
        Order order = new Order(userId, UUID.randomUUID(), new BigDecimal("40.00"), PaymentMethod.WALLET);
        order.addItem(new OrderItem(UUID.randomUUID(), productId, "Test Product", new BigDecimal("40.00"), 1));
        // CREATED -> PAYMENT_PENDING -> CONFIRMED is always legal; walk there for any status beyond CREATED.
        if (status != OrderStatus.CREATED) {
            order.transitionTo(OrderStatus.PAYMENT_PENDING);
            if (status != OrderStatus.PAYMENT_PENDING) {
                order.transitionTo(OrderStatus.CONFIRMED);
            }
        }
        return orderRepository.saveAndFlush(order);
    }

    @Test
    void savingAnOrder_persistsItsItemsToo() {
        Order saved = newOrder(UUID.randomUUID(), "product-1", OrderStatus.CREATED);

        Order reloaded = orderRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getItems()).hasSize(1);
        assertThat(reloaded.getItems().get(0).getProductId()).isEqualTo("product-1");
        assertThat(reloaded.getTotalAmount()).isEqualByComparingTo("40.00");
    }

    @Test
    void existsByUserIdAndStatusAndItems_ProductId_onlyMatchesDeliveredOrdersForThatProduct() {
        UUID userId = UUID.randomUUID();
        Order confirmed = newOrder(userId, "product-1", OrderStatus.CONFIRMED);
        // Walk this one all the way to DELIVERED.
        confirmed.transitionTo(OrderStatus.MERCHANT_ACCEPTED);
        confirmed.transitionTo(OrderStatus.PREPARING);
        confirmed.transitionTo(OrderStatus.READY_FOR_PICKUP);
        confirmed.transitionTo(OrderStatus.DELIVERY_ASSIGNED);
        confirmed.transitionTo(OrderStatus.OUT_FOR_DELIVERY);
        confirmed.transitionTo(OrderStatus.DELIVERED);
        orderRepository.saveAndFlush(confirmed);

        newOrder(userId, "product-2", OrderStatus.CONFIRMED); // same user, different product, not delivered

        assertThat(orderRepository.existsByUserIdAndStatusAndItems_ProductId(userId, OrderStatus.DELIVERED, "product-1")).isTrue();
        assertThat(orderRepository.existsByUserIdAndStatusAndItems_ProductId(userId, OrderStatus.DELIVERED, "product-2")).isFalse();
        assertThat(orderRepository.existsByUserIdAndStatusAndItems_ProductId(UUID.randomUUID(), OrderStatus.DELIVERED, "product-1")).isFalse();
    }

    @Test
    void countByStatusAndUpdatedAtBefore_onlyCountsOrdersOlderThanTheCutoff() {
        UUID userId = UUID.randomUUID();
        Order readyNow = newOrder(userId, "product-1", OrderStatus.CONFIRMED);
        readyNow.transitionTo(OrderStatus.MERCHANT_ACCEPTED);
        readyNow.transitionTo(OrderStatus.PREPARING);
        readyNow.transitionTo(OrderStatus.READY_FOR_PICKUP);
        orderRepository.saveAndFlush(readyNow);

        Instant future = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);

        // Not stale yet by a cutoff in the past (order was updated after that cutoff).
        assertThat(orderRepository.countByStatusAndUpdatedAtBefore(OrderStatus.READY_FOR_PICKUP, past)).isEqualTo(0);
        // Stale by a cutoff in the future (order was updated before that cutoff).
        assertThat(orderRepository.countByStatusAndUpdatedAtBefore(OrderStatus.READY_FOR_PICKUP, future)).isEqualTo(1);
        // Wrong status entirely never counts, regardless of cutoff.
        assertThat(orderRepository.countByStatusAndUpdatedAtBefore(OrderStatus.DELIVERED, future)).isEqualTo(0);
    }
}
