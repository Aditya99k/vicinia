package com.vicinia.inventoryservice.dto;

import com.vicinia.inventoryservice.domain.Reservation;

import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID orderId,
        UUID listingId,
        int quantity,
        String status
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(), reservation.getOrderId(), reservation.getListingId(),
                reservation.getQuantity(), reservation.getStatus().name()
        );
    }
}
