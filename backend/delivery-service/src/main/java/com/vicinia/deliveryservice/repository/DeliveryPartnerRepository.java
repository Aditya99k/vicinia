package com.vicinia.deliveryservice.repository;

import com.vicinia.deliveryservice.domain.DeliveryPartner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, UUID> {
    Optional<DeliveryPartner> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
