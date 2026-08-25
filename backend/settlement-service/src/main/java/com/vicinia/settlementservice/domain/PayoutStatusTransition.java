package com.vicinia.settlementservice.domain;

import com.vicinia.settlementservice.exception.IllegalPayoutStatusException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.vicinia.settlementservice.domain.PayoutStatus.FAILED;
import static com.vicinia.settlementservice.domain.PayoutStatus.PAID;
import static com.vicinia.settlementservice.domain.PayoutStatus.PENDING;
import static com.vicinia.settlementservice.domain.PayoutStatus.PROCESSING;

/** Same pattern as MerchantStatusTransition (Stage 3), OrderStatusTransition (Stage 8), DeliveryTaskStatusTransition (Stage 11). */
public final class PayoutStatusTransition {

    private static final Map<PayoutStatus, Set<PayoutStatus>> ALLOWED = new EnumMap<>(PayoutStatus.class);

    static {
        ALLOWED.put(PENDING, EnumSet.of(PROCESSING));
        ALLOWED.put(PROCESSING, EnumSet.of(PAID, FAILED));
        ALLOWED.put(PAID, EnumSet.noneOf(PayoutStatus.class));
        ALLOWED.put(FAILED, EnumSet.noneOf(PayoutStatus.class));
    }

    private PayoutStatusTransition() {
    }

    public static void assertAllowed(PayoutStatus from, PayoutStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalPayoutStatusException(from, to);
        }
    }
}
