package com.vicinia.paymentservice.repository;

import com.vicinia.paymentservice.domain.TransactionType;
import com.vicinia.paymentservice.domain.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    Optional<WalletTransaction> findByOrderIdAndType(UUID orderId, TransactionType type);

    /** ON CONFLICT DO NOTHING — see coupon-service's CouponUsageRepository.insertIfAbsent for why this beats a caught DataIntegrityViolationException under Postgres. */
    @Modifying
    @Query(value = "INSERT INTO wallet_transactions (id, user_id, order_id, type, amount, created_at) " +
            "VALUES (:id, :userId, :orderId, :type, :amount, :createdAt) " +
            "ON CONFLICT (order_id, type) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("userId") UUID userId, @Param("orderId") UUID orderId,
                        @Param("type") String type, @Param("amount") BigDecimal amount, @Param("createdAt") Instant createdAt);
}
