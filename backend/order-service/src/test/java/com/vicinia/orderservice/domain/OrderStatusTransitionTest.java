package com.vicinia.orderservice.domain;

import com.vicinia.orderservice.exception.IllegalOrderStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Stage 17: the first automated test in this project for the state-machine
 * pattern reused across order-service (this class), merchant-service,
 * delivery-service, and settlement-service. Rather than hand-listing every
 * legal/illegal pair, this walks the full OrderStatus x OrderStatus matrix
 * and asserts each cell matches ARCHITECTURE.md §4.5's own lifecycle
 * diagram (reproduced in OrderStatusTransition's own Javadoc) — a
 * transition guard is only actually correct if *every* illegal jump is
 * rejected, not just the ones a hand-picked example happens to cover.
 */
class OrderStatusTransitionTest {

    private static final java.util.Map<OrderStatus, Set<OrderStatus>> LEGAL = java.util.Map.ofEntries(
            java.util.Map.entry(OrderStatus.CREATED, EnumSet.of(OrderStatus.PAYMENT_PENDING, OrderStatus.CANCELLED)),
            java.util.Map.entry(OrderStatus.PAYMENT_PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.PAYMENT_FAILED)),
            java.util.Map.entry(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.MERCHANT_ACCEPTED, OrderStatus.MERCHANT_REJECTED, OrderStatus.CANCELLED)),
            java.util.Map.entry(OrderStatus.MERCHANT_ACCEPTED, EnumSet.of(OrderStatus.PREPARING)),
            java.util.Map.entry(OrderStatus.PREPARING, EnumSet.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.CANCELLED)),
            java.util.Map.entry(OrderStatus.READY_FOR_PICKUP, EnumSet.of(OrderStatus.DELIVERY_ASSIGNED, OrderStatus.CANCELLED)),
            java.util.Map.entry(OrderStatus.DELIVERY_ASSIGNED, EnumSet.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.CANCELLED)),
            java.util.Map.entry(OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED)),
            java.util.Map.entry(OrderStatus.MERCHANT_REJECTED, EnumSet.of(OrderStatus.REFUND_PENDING)),
            java.util.Map.entry(OrderStatus.CANCELLED, EnumSet.of(OrderStatus.REFUND_PENDING)),
            java.util.Map.entry(OrderStatus.REFUND_PENDING, EnumSet.of(OrderStatus.REFUNDED))
    );

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    void everyLegalTransitionSucceedsAndEveryOtherTargetIsRejected(OrderStatus from) {
        Set<OrderStatus> legalTargets = LEGAL.getOrDefault(from, Set.of());

        for (OrderStatus to : OrderStatus.values()) {
            if (legalTargets.contains(to)) {
                assertThatCode(() -> OrderStatusTransition.assertAllowed(from, to))
                        .as("%s -> %s should be legal", from, to)
                        .doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> OrderStatusTransition.assertAllowed(from, to))
                        .as("%s -> %s should be illegal", from, to)
                        .isInstanceOf(IllegalOrderStatusTransitionException.class);
            }
        }
    }

    @Test
    void terminalStatesAcceptNoTransitionAtAll() {
        for (OrderStatus terminal : Set.of(OrderStatus.PAYMENT_FAILED, OrderStatus.DELIVERED, OrderStatus.REFUNDED)) {
            for (OrderStatus to : OrderStatus.values()) {
                assertThatThrownBy(() -> OrderStatusTransition.assertAllowed(terminal, to))
                        .isInstanceOf(IllegalOrderStatusTransitionException.class);
            }
        }
    }
}
