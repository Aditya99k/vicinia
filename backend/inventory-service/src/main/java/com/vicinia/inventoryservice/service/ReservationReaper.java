package com.vicinia.inventoryservice.service;

import com.vicinia.inventoryservice.domain.Reservation;
import com.vicinia.inventoryservice.domain.ReservationStatus;
import com.vicinia.inventoryservice.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * ADR 0003 — a DB-driven scheduled reaper, not Redis TTL/keyspace-notify
 * eviction: Redis expiry events aren't guaranteed delivery, this is boring
 * but reliable and survives a restart (a missed cycle just catches up on
 * the next one, since the query is always "still PAYMENT_PENDING and older
 * than N minutes", not an event that fires once and can be missed).
 */
@Component
public class ReservationReaper {

    private static final Logger log = LoggerFactory.getLogger(ReservationReaper.class);

    private final ReservationRepository reservationRepository;
    private final InventoryService inventoryService;
    private final int timeoutMinutes;

    public ReservationReaper(ReservationRepository reservationRepository,
                              InventoryService inventoryService,
                              @Value("${vicinia.inventory.reservation-timeout-minutes:15}") int timeoutMinutes) {
        this.reservationRepository = reservationRepository;
        this.inventoryService = inventoryService;
        this.timeoutMinutes = timeoutMinutes;
    }

    @Scheduled(fixedDelayString = "${vicinia.inventory.reaper-interval-ms:90000}")
    public void releaseExpiredReservations() {
        Instant cutoff = Instant.now().minus(timeoutMinutes, ChronoUnit.MINUTES);
        List<Reservation> stale = reservationRepository.findByStatusAndReservedAtBefore(ReservationStatus.PAYMENT_PENDING, cutoff);
        if (stale.isEmpty()) {
            return;
        }
        stale.forEach(inventoryService::releaseOne);
        log.info("Reservation reaper released {} stale reservation(s) older than {} minute(s)", stale.size(), timeoutMinutes);
    }
}
