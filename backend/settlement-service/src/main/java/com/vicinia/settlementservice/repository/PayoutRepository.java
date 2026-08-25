package com.vicinia.settlementservice.repository;

import com.vicinia.settlementservice.domain.Payout;
import com.vicinia.settlementservice.domain.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PayoutRepository extends JpaRepository<Payout, UUID> {

    List<Payout> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    List<Payout> findByStatus(PayoutStatus status);

    List<Payout> findByStatusAndUpdatedAtBefore(PayoutStatus status, Instant cutoff);

    List<Payout> findAllByOrderByCreatedAtDesc();
}
