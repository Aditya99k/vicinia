package com.vicinia.deliveryservice.repository;

import com.vicinia.deliveryservice.domain.DeliveryTask;
import com.vicinia.deliveryservice.domain.DeliveryTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryTaskRepository extends JpaRepository<DeliveryTask, UUID> {
    Optional<DeliveryTask> findByOrderId(UUID orderId);

    List<DeliveryTask> findByStatus(DeliveryTaskStatus status);
}
