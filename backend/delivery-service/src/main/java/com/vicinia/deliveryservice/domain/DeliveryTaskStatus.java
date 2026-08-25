package com.vicinia.deliveryservice.domain;

/**
 * No separate REJECTED state — a partner rejecting an assignment cycles
 * the task back to PENDING_ASSIGNMENT (with that partner excluded from the
 * next search), since the order still needs a delivery; "rejected" isn't a
 * fact about the order, only about one partner's one declined offer.
 */
public enum DeliveryTaskStatus {
    PENDING_ASSIGNMENT,
    ASSIGNED,
    ACCEPTED,
    PICKED_UP,
    DELIVERED
}
