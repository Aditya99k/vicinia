package com.vicinia.merchantservice.domain;

/**
 * ARCHITECTURE.md §4.1. UNDER_REVIEW was cut per ADR 0010 — no behavioral
 * difference from PENDING_REVIEW, cosmetic-only distinction.
 *
 * <pre>
 * PENDING_REVIEW → APPROVED → ONBOARDING → LIVE
 *               ↓
 *            REJECTED (terminal)
 *
 * LIVE → SUSPENDED (admin-initiated, reversible → LIVE)
 * LIVE → TEMP_CLOSED (merchant-initiated, reversible → LIVE)
 * LIVE / SUSPENDED / TEMP_CLOSED → PERMANENTLY_CLOSED (terminal)
 * </pre>
 */
public enum MerchantStatus {
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    ONBOARDING,
    LIVE,
    SUSPENDED,
    TEMP_CLOSED,
    PERMANENTLY_CLOSED
}
