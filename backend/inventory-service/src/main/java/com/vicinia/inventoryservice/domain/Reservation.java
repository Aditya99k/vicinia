package com.vicinia.inventoryservice.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * One row per (orderId, listingId) pair — the unique constraint is what
 * makes reserve idempotent (ARCHITECTURE.md §11: "re-reserving is a no-op,
 * not a double-decrement"). orderId is caller-supplied and, until Stage 8's
 * order-service exists to be the real caller, is whatever UUID a test/future
 * caller uses to group a set of reservation lines together as one order.
 */
@Entity
@Table(name = "reservations", uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "listing_id"}))
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID orderId;
    private UUID listingId;
    private int quantity;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status = ReservationStatus.PAYMENT_PENDING;

    private Instant reservedAt = Instant.now();
    private Instant resolvedAt;

    protected Reservation() {
    }

    public Reservation(UUID orderId, UUID listingId, int quantity) {
        this.orderId = orderId;
        this.listingId = listingId;
        this.quantity = quantity;
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
        this.resolvedAt = Instant.now();
    }

    public void release() {
        this.status = ReservationStatus.RELEASED;
        this.resolvedAt = Instant.now();
    }

    // --- getters ---

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getListingId() {
        return listingId;
    }

    public int getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
