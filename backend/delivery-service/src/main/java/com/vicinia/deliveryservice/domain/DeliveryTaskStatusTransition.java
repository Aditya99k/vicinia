package com.vicinia.deliveryservice.domain;

import com.vicinia.deliveryservice.exception.IllegalDeliveryTaskStatusException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.vicinia.deliveryservice.domain.DeliveryTaskStatus.ACCEPTED;
import static com.vicinia.deliveryservice.domain.DeliveryTaskStatus.ASSIGNED;
import static com.vicinia.deliveryservice.domain.DeliveryTaskStatus.DELIVERED;
import static com.vicinia.deliveryservice.domain.DeliveryTaskStatus.PENDING_ASSIGNMENT;
import static com.vicinia.deliveryservice.domain.DeliveryTaskStatus.PICKED_UP;

/** Same pattern as MerchantStatusTransition (Stage 3), OrderStatusTransition (Stage 8), OrderTaskStatusTransition (Stage 11). */
public final class DeliveryTaskStatusTransition {

    private static final Map<DeliveryTaskStatus, Set<DeliveryTaskStatus>> ALLOWED = new EnumMap<>(DeliveryTaskStatus.class);

    static {
        ALLOWED.put(PENDING_ASSIGNMENT, EnumSet.of(ASSIGNED));
        ALLOWED.put(ASSIGNED, EnumSet.of(ACCEPTED, PENDING_ASSIGNMENT));
        ALLOWED.put(ACCEPTED, EnumSet.of(PICKED_UP));
        ALLOWED.put(PICKED_UP, EnumSet.of(DELIVERED));
        ALLOWED.put(DELIVERED, EnumSet.noneOf(DeliveryTaskStatus.class));
    }

    private DeliveryTaskStatusTransition() {
    }

    public static void assertAllowed(DeliveryTaskStatus from, DeliveryTaskStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalDeliveryTaskStatusException(from, to);
        }
    }
}
