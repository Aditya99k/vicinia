package com.vicinia.settlementservice.service;

import com.vicinia.settlementservice.domain.Payout;
import com.vicinia.settlementservice.domain.PayoutStatus;
import com.vicinia.settlementservice.domain.SettlementEntry;
import com.vicinia.settlementservice.domain.SettlementEntryStatus;
import com.vicinia.settlementservice.messaging.SettlementEventPublisher;
import com.vicinia.settlementservice.repository.PayoutRepository;
import com.vicinia.settlementservice.repository.SettlementEntryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SettlementService {

    private final SettlementEntryRepository entryRepository;
    private final PayoutRepository payoutRepository;
    private final SettlementEventPublisher eventPublisher;
    private final BigDecimal commissionRate;
    private final long processingDelayMs;

    public SettlementService(SettlementEntryRepository entryRepository, PayoutRepository payoutRepository,
                              SettlementEventPublisher eventPublisher,
                              @Value("${vicinia.settlement.commission-rate:0.05}") BigDecimal commissionRate,
                              @Value("${vicinia.settlement.payout-processing-delay-ms:60000}") long processingDelayMs) {
        this.entryRepository = entryRepository;
        this.payoutRepository = payoutRepository;
        this.eventPublisher = eventPublisher;
        this.commissionRate = commissionRate;
        this.processingDelayMs = processingDelayMs;
    }

    /** Idempotent on orderId — a redelivered order.delivered (retry, rebalance) never produces a second ledger line. */
    public void recordDelivered(UUID orderId, UUID merchantId, BigDecimal gross) {
        if (entryRepository.existsByOrderId(orderId)) {
            return;
        }
        BigDecimal commission = gross.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = gross.subtract(commission);
        entryRepository.save(new SettlementEntry(orderId, merchantId, gross, commission, net));
    }

    public List<SettlementEntry> mine(UUID merchantId) {
        return entryRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    public List<Payout> payoutsMine(UUID merchantId) {
        return payoutRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    public List<SettlementEntry> allEntries() {
        return entryRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Payout> allPayouts() {
        return payoutRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Groups every merchant's PENDING entries into one new Payout each,
     * summing net amounts. The one deliberate exception to this project's
     * no-@Transactional-in-orchestration doctrine (order-service since
     * Stage 8, delivery-service/merchant-service in Stage 11): that
     * doctrine exists specifically to keep an external call (Kafka
     * publish, another service's REST call) out of a transaction that
     * might still roll back — this method makes no external call at all,
     * it's a single atomic multi-row Postgres write (create one Payout,
     * link N entries to it), which is exactly the case @Transactional is
     * for and the reason the doctrine doesn't apply here.
     */
    @Transactional
    public List<Payout> runBatch() {
        List<Payout> created = new ArrayList<>();
        for (UUID merchantId : entryRepository.findDistinctMerchantIdsByStatus(SettlementEntryStatus.PENDING)) {
            List<SettlementEntry> pending = entryRepository.findByMerchantIdAndStatus(merchantId, SettlementEntryStatus.PENDING);
            if (pending.isEmpty()) {
                continue;
            }
            BigDecimal total = pending.stream().map(SettlementEntry::getNet).reduce(BigDecimal.ZERO, BigDecimal::add);
            Payout payout = payoutRepository.save(new Payout(merchantId, total));
            for (SettlementEntry entry : pending) {
                entry.assignToPayout(payout.getId());
            }
            entryRepository.saveAll(pending);
            created.add(payout);
        }
        return created;
    }

    /** Advances PENDING->PROCESSING immediately (picked up), then PROCESSING->PAID once processingDelayMs has passed — same reaper-style scheduled-job pattern as Stage 5's ReservationReaper and Stage 11's AssignmentReaper. */
    public void runProcessorTick() {
        for (Payout payout : payoutRepository.findByStatus(PayoutStatus.PENDING)) {
            payout.transitionTo(PayoutStatus.PROCESSING);
            payoutRepository.save(payout);
        }

        Instant cutoff = Instant.now().minusMillis(processingDelayMs);
        for (Payout payout : payoutRepository.findByStatusAndUpdatedAtBefore(PayoutStatus.PROCESSING, cutoff)) {
            payout.transitionTo(PayoutStatus.PAID);
            Payout saved = payoutRepository.save(payout);
            eventPublisher.publishCompleted(saved.getId(), saved.getMerchantId(), saved.getTotalAmount());
        }
    }
}
