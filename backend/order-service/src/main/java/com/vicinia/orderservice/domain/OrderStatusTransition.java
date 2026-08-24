package com.vicinia.orderservice.domain;

import com.vicinia.orderservice.exception.IllegalOrderStatusTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.vicinia.orderservice.domain.OrderStatus.CANCELLED;
import static com.vicinia.orderservice.domain.OrderStatus.CONFIRMED;
import static com.vicinia.orderservice.domain.OrderStatus.CREATED;
import static com.vicinia.orderservice.domain.OrderStatus.DELIVERED;
import static com.vicinia.orderservice.domain.OrderStatus.DELIVERY_ASSIGNED;
import static com.vicinia.orderservice.domain.OrderStatus.MERCHANT_ACCEPTED;
import static com.vicinia.orderservice.domain.OrderStatus.MERCHANT_REJECTED;
import static com.vicinia.orderservice.domain.OrderStatus.OUT_FOR_DELIVERY;
import static com.vicinia.orderservice.domain.OrderStatus.PAYMENT_FAILED;
import static com.vicinia.orderservice.domain.OrderStatus.PAYMENT_PENDING;
import static com.vicinia.orderservice.domain.OrderStatus.PREPARING;
import static com.vicinia.orderservice.domain.OrderStatus.READY_FOR_PICKUP;
import static com.vicinia.orderservice.domain.OrderStatus.REFUNDED;
import static com.vicinia.orderservice.domain.OrderStatus.REFUND_PENDING;

/** The one place that decides whether a status change is legal — same pattern as merchant-service's MerchantStatusTransition (Stage 3). */
public final class OrderStatusTransition {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(CREATED, EnumSet.of(PAYMENT_PENDING, CANCELLED));
        ALLOWED.put(PAYMENT_PENDING, EnumSet.of(CONFIRMED, PAYMENT_FAILED));
        ALLOWED.put(CONFIRMED, EnumSet.of(MERCHANT_ACCEPTED, MERCHANT_REJECTED, CANCELLED));
        ALLOWED.put(MERCHANT_ACCEPTED, EnumSet.of(PREPARING));
        ALLOWED.put(PREPARING, EnumSet.of(READY_FOR_PICKUP, CANCELLED));
        ALLOWED.put(READY_FOR_PICKUP, EnumSet.of(DELIVERY_ASSIGNED));
        ALLOWED.put(DELIVERY_ASSIGNED, EnumSet.of(OUT_FOR_DELIVERY));
        ALLOWED.put(OUT_FOR_DELIVERY, EnumSet.of(DELIVERED));
        ALLOWED.put(MERCHANT_REJECTED, EnumSet.of(REFUND_PENDING));
        ALLOWED.put(CANCELLED, EnumSet.of(REFUND_PENDING));
        ALLOWED.put(REFUND_PENDING, EnumSet.of(REFUNDED));
        ALLOWED.put(PAYMENT_FAILED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(DELIVERED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(REFUNDED, EnumSet.noneOf(OrderStatus.class));
    }

    private OrderStatusTransition() {
    }

    public static void assertAllowed(OrderStatus from, OrderStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalOrderStatusTransitionException(from, to);
        }
    }
}
