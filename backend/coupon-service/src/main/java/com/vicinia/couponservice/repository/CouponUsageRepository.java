package com.vicinia.couponservice.repository;

import com.vicinia.couponservice.domain.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, UUID> {

    Optional<CouponUsage> findByCouponIdAndOrderId(UUID couponId, UUID orderId);

    long countByCouponIdAndUserId(UUID couponId, UUID userId);

    /**
     * ON CONFLICT DO NOTHING instead of a plain INSERT + catching a unique-
     * constraint violation in Java: under Postgres, any statement error —
     * including a constraint violation — aborts the entire surrounding
     * transaction until a ROLLBACK, so a caught DataIntegrityViolationException
     * would leave every subsequent statement in the same @Transactional
     * method failing with "current transaction is aborted." ON CONFLICT
     * never raises an error in the first place, so CouponService.apply can
     * cleanly detect and recover from the race (two concurrent applies for
     * the same coupon+order) without needing savepoints.
     */
    @Modifying
    @Query(value = "INSERT INTO coupon_usages (id, coupon_id, user_id, order_id, discount_amount, used_at) " +
            "VALUES (:id, :couponId, :userId, :orderId, :discountAmount, :usedAt) " +
            "ON CONFLICT (coupon_id, order_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("couponId") UUID couponId, @Param("userId") UUID userId,
                        @Param("orderId") UUID orderId, @Param("discountAmount") BigDecimal discountAmount,
                        @Param("usedAt") Instant usedAt);
}
