package com.vicinia.merchantservice.domain;

/** A merchant's own view of one order that needs their action — separate from order-service's own canonical OrderStatus (ADR 0004: each service tracks its own task state, order-service mirrors it via events). */
public enum OrderTaskStatus {
    PENDING_ACCEPTANCE,
    ACCEPTED,
    REJECTED,
    READY
}
