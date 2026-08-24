package com.vicinia.merchantservice.domain;

import com.vicinia.merchantservice.exception.IllegalStatusTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.vicinia.merchantservice.domain.MerchantStatus.APPROVED;
import static com.vicinia.merchantservice.domain.MerchantStatus.LIVE;
import static com.vicinia.merchantservice.domain.MerchantStatus.ONBOARDING;
import static com.vicinia.merchantservice.domain.MerchantStatus.PENDING_REVIEW;
import static com.vicinia.merchantservice.domain.MerchantStatus.PERMANENTLY_CLOSED;
import static com.vicinia.merchantservice.domain.MerchantStatus.REJECTED;
import static com.vicinia.merchantservice.domain.MerchantStatus.SUSPENDED;
import static com.vicinia.merchantservice.domain.MerchantStatus.TEMP_CLOSED;

/**
 * The one place that decides whether a status change is legal — every
 * transition in the service goes through {@link #assertAllowed}, so an
 * illegal jump (e.g. PENDING_REVIEW straight to LIVE) is rejected the same
 * way no matter which endpoint tried it.
 */
public final class MerchantStatusTransition {

    private static final Map<MerchantStatus, Set<MerchantStatus>> ALLOWED = new EnumMap<>(MerchantStatus.class);

    static {
        ALLOWED.put(PENDING_REVIEW, EnumSet.of(APPROVED, REJECTED));
        ALLOWED.put(APPROVED, EnumSet.of(ONBOARDING));
        ALLOWED.put(REJECTED, EnumSet.noneOf(MerchantStatus.class));
        ALLOWED.put(ONBOARDING, EnumSet.of(LIVE));
        ALLOWED.put(LIVE, EnumSet.of(SUSPENDED, TEMP_CLOSED, PERMANENTLY_CLOSED));
        ALLOWED.put(SUSPENDED, EnumSet.of(LIVE, PERMANENTLY_CLOSED));
        ALLOWED.put(TEMP_CLOSED, EnumSet.of(LIVE, PERMANENTLY_CLOSED));
        ALLOWED.put(PERMANENTLY_CLOSED, EnumSet.noneOf(MerchantStatus.class));
    }

    private MerchantStatusTransition() {
    }

    public static void assertAllowed(MerchantStatus from, MerchantStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalStatusTransitionException(from, to);
        }
    }
}
