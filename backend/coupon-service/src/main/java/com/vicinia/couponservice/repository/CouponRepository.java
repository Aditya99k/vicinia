package com.vicinia.couponservice.repository;

import com.vicinia.couponservice.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Coupon> findAllByOrderByCreatedAtDesc();

    /**
     * ADR 0002's pattern applied to coupons (ARCHITECTURE.md §6 — "same
     * family as inventory"): one atomic conditional UPDATE, rows-affected
     * tells the caller whether it won, not a read-then-write race in
     * application code. usageLimit IS NULL means unlimited.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Coupon c SET c.usageCount = c.usageCount + 1, c.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE c.id = :id AND (c.usageLimit IS NULL OR c.usageCount < c.usageLimit)")
    int tryIncrementUsage(@Param("id") UUID id);

    /** Compensates a spurious increment when two concurrent apply() calls for the same (coupon, order) both pass the increment but only one wins the usage-row insert — see CouponService.apply. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Coupon c SET c.usageCount = c.usageCount - 1, c.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE c.id = :id AND c.usageCount > 0")
    int tryDecrementUsage(@Param("id") UUID id);
}
