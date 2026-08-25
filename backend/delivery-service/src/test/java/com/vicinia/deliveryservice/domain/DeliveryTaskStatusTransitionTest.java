package com.vicinia.deliveryservice.domain;

import com.vicinia.deliveryservice.exception.IllegalDeliveryTaskStatusException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Same full-matrix approach as order-service's OrderStatusTransitionTest.
 * Deliberately confirms ASSIGNED -> PENDING_ASSIGNMENT is legal (the
 * reject-and-reassign path, Stage 11) and that there's no separate
 * REJECTED state at all in this matrix — a partner declining is a return
 * trip, not a terminal branch.
 */
class DeliveryTaskStatusTransitionTest {

    private static final Map<DeliveryTaskStatus, Set<DeliveryTaskStatus>> LEGAL = Map.of(
            DeliveryTaskStatus.PENDING_ASSIGNMENT, EnumSet.of(DeliveryTaskStatus.ASSIGNED),
            DeliveryTaskStatus.ASSIGNED, EnumSet.of(DeliveryTaskStatus.ACCEPTED, DeliveryTaskStatus.PENDING_ASSIGNMENT),
            DeliveryTaskStatus.ACCEPTED, EnumSet.of(DeliveryTaskStatus.PICKED_UP),
            DeliveryTaskStatus.PICKED_UP, EnumSet.of(DeliveryTaskStatus.DELIVERED)
    );

    @ParameterizedTest
    @EnumSource(DeliveryTaskStatus.class)
    void everyLegalTransitionSucceedsAndEveryOtherTargetIsRejected(DeliveryTaskStatus from) {
        Set<DeliveryTaskStatus> legalTargets = LEGAL.getOrDefault(from, Set.of());

        for (DeliveryTaskStatus to : DeliveryTaskStatus.values()) {
            if (legalTargets.contains(to)) {
                assertThatCode(() -> DeliveryTaskStatusTransition.assertAllowed(from, to))
                        .as("%s -> %s should be legal", from, to)
                        .doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> DeliveryTaskStatusTransition.assertAllowed(from, to))
                        .as("%s -> %s should be illegal", from, to)
                        .isInstanceOf(IllegalDeliveryTaskStatusException.class);
            }
        }
    }
}
