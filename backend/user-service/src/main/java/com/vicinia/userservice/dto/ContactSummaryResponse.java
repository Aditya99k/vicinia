package com.vicinia.userservice.dto;

/** Minimal cross-service contact lookup — just enough for order-service to show a customer "who's delivering this and how to reach them", never the full profile (no email). */
public record ContactSummaryResponse(String fullName, String phone) {
}
