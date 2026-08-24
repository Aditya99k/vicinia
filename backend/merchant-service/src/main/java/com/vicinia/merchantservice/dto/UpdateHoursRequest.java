package com.vicinia.merchantservice.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record UpdateHoursRequest(
        @NotNull LocalTime openTime,
        @NotNull LocalTime closeTime
) {
}
