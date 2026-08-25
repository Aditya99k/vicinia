package com.vicinia.settlementservice.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Simulated payout execution — same reaper-style scheduled-job pattern as Stage 5's ReservationReaper and Stage 11's AssignmentReaper. */
@Component
public class PayoutProcessor {

    private final SettlementService settlementService;

    public PayoutProcessor(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @Scheduled(fixedDelayString = "${vicinia.settlement.payout-processor-interval-ms:30000}")
    public void run() {
        settlementService.runProcessorTick();
    }
}
