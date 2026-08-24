package com.vicinia.inventoryservice.repository;

import com.vicinia.inventoryservice.domain.Reservation;
import com.vicinia.inventoryservice.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByOrderIdAndListingId(UUID orderId, UUID listingId);

    List<Reservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);

    List<Reservation> findByStatusAndReservedAtBefore(ReservationStatus status, Instant cutoff);
}
