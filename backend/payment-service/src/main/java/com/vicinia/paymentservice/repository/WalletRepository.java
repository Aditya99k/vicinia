package com.vicinia.paymentservice.repository;

import com.vicinia.paymentservice.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    /** ADR 0002's pattern again — same family as inventory and coupons. Rows-affected of 0 means insufficient balance, not an exception. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Wallet w SET w.balance = w.balance - :amount, w.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE w.userId = :userId AND w.balance >= :amount")
    int tryDebit(@Param("userId") UUID userId, @Param("amount") BigDecimal amount);

    /** Credit always succeeds if the wallet exists — used by both topup and refund. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Wallet w SET w.balance = w.balance + :amount, w.updatedAt = CURRENT_TIMESTAMP WHERE w.userId = :userId")
    int credit(@Param("userId") UUID userId, @Param("amount") BigDecimal amount);
}
