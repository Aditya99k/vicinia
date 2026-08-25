package com.vicinia.merchantservice.domain;

import com.vicinia.merchantservice.exception.IllegalStatusTransitionException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Same full-matrix approach as order-service's OrderStatusTransitionTest — see that class for why. */
class MerchantStatusTransitionTest {

    private static final Map<MerchantStatus, Set<MerchantStatus>> LEGAL = Map.of(
            MerchantStatus.PENDING_REVIEW, EnumSet.of(MerchantStatus.APPROVED, MerchantStatus.REJECTED),
            MerchantStatus.APPROVED, EnumSet.of(MerchantStatus.ONBOARDING),
            MerchantStatus.ONBOARDING, EnumSet.of(MerchantStatus.LIVE),
            MerchantStatus.LIVE, EnumSet.of(MerchantStatus.SUSPENDED, MerchantStatus.TEMP_CLOSED, MerchantStatus.PERMANENTLY_CLOSED),
            MerchantStatus.SUSPENDED, EnumSet.of(MerchantStatus.LIVE, MerchantStatus.PERMANENTLY_CLOSED),
            MerchantStatus.TEMP_CLOSED, EnumSet.of(MerchantStatus.LIVE, MerchantStatus.PERMANENTLY_CLOSED)
    );

    @ParameterizedTest
    @EnumSource(MerchantStatus.class)
    void everyLegalTransitionSucceedsAndEveryOtherTargetIsRejected(MerchantStatus from) {
        Set<MerchantStatus> legalTargets = LEGAL.getOrDefault(from, Set.of());

        for (MerchantStatus to : MerchantStatus.values()) {
            if (legalTargets.contains(to)) {
                assertThatCode(() -> MerchantStatusTransition.assertAllowed(from, to))
                        .as("%s -> %s should be legal", from, to)
                        .doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> MerchantStatusTransition.assertAllowed(from, to))
                        .as("%s -> %s should be illegal", from, to)
                        .isInstanceOf(IllegalStatusTransitionException.class);
            }
        }
    }
}
