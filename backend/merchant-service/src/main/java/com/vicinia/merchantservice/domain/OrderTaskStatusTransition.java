package com.vicinia.merchantservice.domain;

import com.vicinia.merchantservice.exception.IllegalOrderTaskStatusException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.vicinia.merchantservice.domain.OrderTaskStatus.ACCEPTED;
import static com.vicinia.merchantservice.domain.OrderTaskStatus.COMPLETED;
import static com.vicinia.merchantservice.domain.OrderTaskStatus.PENDING_ACCEPTANCE;
import static com.vicinia.merchantservice.domain.OrderTaskStatus.READY;
import static com.vicinia.merchantservice.domain.OrderTaskStatus.REJECTED;

/** Same pattern as MerchantStatusTransition (Stage 3) and OrderStatusTransition (Stage 8). */
public final class OrderTaskStatusTransition {

    private static final Map<OrderTaskStatus, Set<OrderTaskStatus>> ALLOWED = new EnumMap<>(OrderTaskStatus.class);

    static {
        ALLOWED.put(PENDING_ACCEPTANCE, EnumSet.of(ACCEPTED, REJECTED));
        ALLOWED.put(ACCEPTED, EnumSet.of(READY));
        ALLOWED.put(REJECTED, EnumSet.noneOf(OrderTaskStatus.class));
        ALLOWED.put(READY, EnumSet.of(COMPLETED));
        ALLOWED.put(COMPLETED, EnumSet.noneOf(OrderTaskStatus.class));
    }

    private OrderTaskStatusTransition() {
    }

    public static void assertAllowed(OrderTaskStatus from, OrderTaskStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalOrderTaskStatusException(from, to);
        }
    }
}
