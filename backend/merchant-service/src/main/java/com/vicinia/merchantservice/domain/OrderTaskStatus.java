package com.vicinia.merchantservice.domain;

/**
 * A merchant's own view of one order that needs their action — separate
 * from order-service's own canonical OrderStatus (ADR 0004: each service
 * tracks its own task state, order-service mirrors it via events). READY
 * stays visible in the merchant's own queue (Stage 18) so whoever's at the
 * counter can verify a rider's pickup against the order id — it only
 * leaves once COMPLETED, driven by delivery-service's own delivery.delivered
 * event, not by any action the merchant takes here.
 */
public enum OrderTaskStatus {
    PENDING_ACCEPTANCE,
    ACCEPTED,
    REJECTED,
    READY,
    COMPLETED
}
