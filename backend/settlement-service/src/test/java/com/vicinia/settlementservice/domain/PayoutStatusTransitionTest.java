package com.vicinia.settlementservice.domain;

import com.vicinia.settlementservice.exception.IllegalPayoutStatusException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Same full-matrix approach as order-service's OrderStatusTransitionTest. */
class PayoutStatusTransitionTest {

    private static final Map<PayoutStatus, Set<PayoutStatus>> LEGAL = Map.of(
            PayoutStatus.PENDING, EnumSet.of(PayoutStatus.PROCESSING),
            PayoutStatus.PROCESSING, EnumSet.of(PayoutStatus.PAID, PayoutStatus.FAILED)
    );

    @ParameterizedTest
    @EnumSource(PayoutStatus.class)
    void everyLegalTransitionSucceedsAndEveryOtherTargetIsRejected(PayoutStatus from) {
        Set<PayoutStatus> legalTargets = LEGAL.getOrDefault(from, Set.of());

        for (PayoutStatus to : PayoutStatus.values()) {
            if (legalTargets.contains(to)) {
                assertThatCode(() -> PayoutStatusTransition.assertAllowed(from, to))
                        .as("%s -> %s should be legal", from, to)
                        .doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> PayoutStatusTransition.assertAllowed(from, to))
                        .as("%s -> %s should be illegal", from, to)
                        .isInstanceOf(IllegalPayoutStatusException.class);
            }
        }
    }

    @org.junit.jupiter.api.Test
    void paidAndFailedAreTerminal() {
        for (PayoutStatus terminal : Set.of(PayoutStatus.PAID, PayoutStatus.FAILED)) {
            for (PayoutStatus to : PayoutStatus.values()) {
                assertThatThrownBy(() -> PayoutStatusTransition.assertAllowed(terminal, to))
                        .isInstanceOf(IllegalPayoutStatusException.class);
            }
        }
    }
}
