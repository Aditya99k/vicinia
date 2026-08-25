package com.vicinia.settlementservice.repository;

import com.vicinia.settlementservice.domain.SettlementEntry;
import com.vicinia.settlementservice.domain.SettlementEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SettlementEntryRepository extends JpaRepository<SettlementEntry, UUID> {

    boolean existsByOrderId(UUID orderId);

    List<SettlementEntry> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    List<SettlementEntry> findByMerchantIdAndStatus(UUID merchantId, SettlementEntryStatus status);

    List<SettlementEntry> findByPayoutId(UUID payoutId);

    List<SettlementEntry> findAllByOrderByCreatedAtDesc();

    @Query("select distinct e.merchantId from SettlementEntry e where e.status = :status")
    List<UUID> findDistinctMerchantIdsByStatus(SettlementEntryStatus status);
}
