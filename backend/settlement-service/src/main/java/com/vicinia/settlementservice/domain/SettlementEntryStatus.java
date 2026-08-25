package com.vicinia.settlementservice.domain;

/** PENDING until the batch job groups it into a Payout; SETTLED afterward. No other states — a settled entry is never un-settled, it's a historical ledger line. */
public enum SettlementEntryStatus {
    PENDING,
    SETTLED
}
