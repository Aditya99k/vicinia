package com.vicinia.paymentservice.repository;

import com.vicinia.paymentservice.domain.RazorpayPayment;
import com.vicinia.paymentservice.domain.RazorpayPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RazorpayPaymentRepository extends JpaRepository<RazorpayPayment, UUID> {

    Optional<RazorpayPayment> findByOrderId(UUID orderId);

    Optional<RazorpayPayment> findByRazorpayOrderId(String razorpayOrderId);

    /**
     * Resolves a CREATED payment to its terminal state — the atomic
     * conditional UPDATE that makes duplicate webhook delivery safe (ADR
     * 0002's pattern, same family as inventory/coupons/wallet). Only fires
     * (rows-affected 1) the first time; a replayed webhook for an already-
     * resolved payment affects 0 rows, so the caller knows not to publish a
     * second payment.success/failed for the same order.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RazorpayPayment p SET p.status = :status, p.razorpayPaymentId = :razorpayPaymentId, " +
            "p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id AND p.status = com.vicinia.paymentservice.domain.RazorpayPaymentStatus.CREATED")
    int tryResolve(@Param("id") UUID id, @Param("status") RazorpayPaymentStatus status, @Param("razorpayPaymentId") String razorpayPaymentId);
}
