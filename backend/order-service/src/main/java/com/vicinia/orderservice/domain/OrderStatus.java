package com.vicinia.orderservice.domain;

/**
 * The full lifecycle from ARCHITECTURE.md §4.5:
 *
 * <pre>
 * CREATED -> PAYMENT_PENDING -> CONFIRMED -> MERCHANT_ACCEPTED -> PREPARING
 *         -> READY_FOR_PICKUP -> DELIVERY_ASSIGNED -> OUT_FOR_DELIVERY -> DELIVERED
 *
 * Branches:
 *   PAYMENT_PENDING -> PAYMENT_FAILED
 *   CREATED/CONFIRMED/PREPARING -> CANCELLED
 *   MERCHANT_ACCEPTED-eligible -> MERCHANT_REJECTED -> (refund path)
 * </pre>
 *
 * Stage 8 only ever drives CREATED through CONFIRMED/PAYMENT_FAILED/CANCELLED
 * itself — MERCHANT_ACCEPTED onward has no real producer yet (merchant-service
 * has no order-acceptance endpoint, delivery-service doesn't exist until
 * Stage 11). The full enum and transition guard are built now anyway, same
 * as Stage 3's complete MerchantStatusTransition: later stages just need to
 * call transitionTo(X), the rule is already correct and in one place.
 */
public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    CONFIRMED,
    MERCHANT_ACCEPTED,
    PREPARING,
    READY_FOR_PICKUP,
    DELIVERY_ASSIGNED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    PAYMENT_FAILED,
    CANCELLED,
    MERCHANT_REJECTED,
    REFUND_PENDING,
    REFUNDED
}
