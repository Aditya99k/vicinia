package com.vicinia.merchantservice.repository;

import com.vicinia.merchantservice.domain.MerchantOrderTask;
import com.vicinia.merchantservice.domain.OrderTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantOrderTaskRepository extends JpaRepository<MerchantOrderTask, UUID> {
    Optional<MerchantOrderTask> findByOrderId(UUID orderId);

    List<MerchantOrderTask> findByMerchantIdAndStatusOrderByCreatedAtAsc(UUID merchantId, OrderTaskStatus status);
}
