package com.vicinia.userservice.repository;

import com.vicinia.userservice.domain.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByUserId(UUID userId);

    Address findByIdAndUserId(UUID id, UUID userId);

    /** Only one address can be default at a time — clears it on every other address of this user before the caller sets the new one. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.userId = :userId AND a.id <> :excludeId AND a.isDefault = true")
    void clearDefaultForOthers(@Param("userId") UUID userId, @Param("excludeId") UUID excludeId);
}
