package com.vicinia.orderservice.observability;

import com.vicinia.orderservice.domain.OrderStatus;
import com.vicinia.orderservice.repository.OrderRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Stage 16's "stale READY_FOR_PICKUP orders" Grafana panel (ARCHITECTURE.md
 * §15) — a canary for "no delivery partner assigned within N minutes."
 * Pull-based, like every other Micrometer Gauge: Prometheus re-evaluates
 * the query on every scrape, so there's no scheduled job pushing a value —
 * simpler than the reaper pattern used elsewhere in this project for
 * genuinely acting on stale state, since this metric only needs to be
 * read, not acted on.
 */
@Component
public class StaleOrderMetrics {

    private final OrderRepository orderRepository;
    private final long staleThresholdMinutes;

    public StaleOrderMetrics(OrderRepository orderRepository, MeterRegistry meterRegistry,
                              @Value("${vicinia.observability.stale-ready-for-pickup-minutes:15}") long staleThresholdMinutes) {
        this.orderRepository = orderRepository;
        this.staleThresholdMinutes = staleThresholdMinutes;
        Gauge.builder("order.stale.ready_for_pickup", this, StaleOrderMetrics::countStale)
                .description("Orders sitting in READY_FOR_PICKUP for more than " + staleThresholdMinutes + " minutes with no delivery partner assigned")
                .register(meterRegistry);
    }

    private double countStale() {
        Instant cutoff = Instant.now().minus(staleThresholdMinutes, ChronoUnit.MINUTES);
        return orderRepository.countByStatusAndUpdatedAtBefore(OrderStatus.READY_FOR_PICKUP, cutoff);
    }
}
