package com.vicinia.settlementservice.domain;

/** ARCHITECTURE.md §4.7: PENDING -> PROCESSING -> PAID | FAILED. PAID is simulated in V1 (no real bank integration) — the state machine and ledger are real, that's the part that demonstrates the skill. */
public enum PayoutStatus {
    PENDING,
    PROCESSING,
    PAID,
    FAILED
}
