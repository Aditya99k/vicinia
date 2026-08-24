package com.vicinia.inventoryservice.domain;

/** Reservation lifecycle (ARCHITECTURE.md §4.4): RESERVE -> CONFIRM or RELEASE. */
public enum ReservationStatus {
    PAYMENT_PENDING,
    CONFIRMED,
    RELEASED
}
