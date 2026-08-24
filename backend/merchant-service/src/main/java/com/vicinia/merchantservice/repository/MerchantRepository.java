package com.vicinia.merchantservice.repository;

import com.vicinia.merchantservice.domain.Merchant;
import com.vicinia.merchantservice.domain.MerchantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    Optional<Merchant> findByOwnerUserId(UUID ownerUserId);

    boolean existsByOwnerUserId(UUID ownerUserId);

    List<Merchant> findByStatusOrderByCreatedAtAsc(MerchantStatus status);

    List<Merchant> findByStatus(MerchantStatus status);

    List<Merchant> findByStatusAndCityIgnoreCase(MerchantStatus status, String city);
}
