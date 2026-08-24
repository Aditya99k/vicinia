package com.vicinia.inventoryservice.repository;

import com.vicinia.inventoryservice.domain.MerchantListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MerchantListingRepository extends JpaRepository<MerchantListing, UUID> {

    boolean existsByMerchantIdAndProductId(UUID merchantId, String productId);

    List<MerchantListing> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    List<MerchantListing> findByProductIdAndActiveTrue(String productId);

    /**
     * ADR 0002 — the atomic conditional UPDATE the oversell guarantee rests
     * on. Postgres takes a row lock for the duration of this statement, so
     * concurrent callers against the same listing serialize here rather
     * than racing on a read-then-write in application code; the WHERE
     * clause is re-evaluated against the (now-locked, current) row, so the
     * second of two concurrent callers sees the first one's decrement
     * before its own predicate is checked. Returns 0 rows affected, not an
     * exception, when stock is insufficient — the caller checks the count.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE MerchantListing l SET l.availableStock = l.availableStock - :qty, " +
            "l.reservedStock = l.reservedStock + :qty, l.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE l.id = :id AND l.availableStock >= :qty")
    int tryReserve(@Param("id") UUID id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MerchantListing l SET l.reservedStock = l.reservedStock - :qty, " +
            "l.updatedAt = CURRENT_TIMESTAMP WHERE l.id = :id AND l.reservedStock >= :qty")
    int tryConfirm(@Param("id") UUID id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MerchantListing l SET l.availableStock = l.availableStock + :qty, " +
            "l.reservedStock = l.reservedStock - :qty, l.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE l.id = :id AND l.reservedStock >= :qty")
    int tryRelease(@Param("id") UUID id, @Param("qty") int qty);
}
