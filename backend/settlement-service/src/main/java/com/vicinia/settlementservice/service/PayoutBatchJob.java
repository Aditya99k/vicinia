package com.vicinia.settlementservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** The automatic, "e.g. daily" cadence (ARCHITECTURE.md §4.7) — real ops/testing use the on-demand POST /api/settlements/admin/payouts/run-batch instead of waiting for this. */
@Component
public class PayoutBatchJob {

    private static final Logger log = LoggerFactory.getLogger(PayoutBatchJob.class);

    private final SettlementService settlementService;

    public PayoutBatchJob(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @Scheduled(fixedDelayString = "${vicinia.settlement.payout-batch-interval-ms:86400000}")
    public void run() {
        var created = settlementService.runBatch();
        if (!created.isEmpty()) {
            log.info("Payout batch job created {} payout(s)", created.size());
        }
    }
}
