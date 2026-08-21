package com.vicinia.userservice.repository;

import com.vicinia.userservice.domain.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByUserId(UUID userId);

    Address findByIdAndUserId(UUID id, UUID userId);
}
