package com.vicinia.paymentservice.service;

import com.vicinia.paymentservice.domain.TransactionType;
import com.vicinia.paymentservice.domain.Wallet;
import com.vicinia.paymentservice.domain.WalletTransaction;
import com.vicinia.paymentservice.exception.InsufficientBalanceException;
import com.vicinia.paymentservice.exception.WalletNotFoundException;
import com.vicinia.paymentservice.messaging.PaymentEventPublisher;
import com.vicinia.paymentservice.repository.WalletRepository;
import com.vicinia.paymentservice.repository.WalletTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final PaymentEventPublisher eventPublisher;

    public WalletService(WalletRepository walletRepository,
                          WalletTransactionRepository transactionRepository,
                          PaymentEventPublisher eventPublisher) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void provisionIfAbsent(UUID userId) {
        if (walletRepository.existsByUserId(userId)) {
            log.debug("Wallet for {} already exists — skipping duplicate user.registered", userId);
            return;
        }
        walletRepository.save(new Wallet(userId));
        log.info("Provisioned wallet for {}", userId);
    }

    public Wallet getByUserId(UUID userId) {
        return walletRepository.findByUserId(userId).orElseThrow(() -> new WalletNotFoundException(userId));
    }

    @Transactional
    public WalletTransaction topup(UUID userId, BigDecimal amount) {
        getByUserId(userId); // 404 if no wallet — shouldn't happen post-provisioning, but don't silently credit a nonexistent user
        walletRepository.credit(userId, amount);
        return transactionRepository.save(new WalletTransaction(userId, null, TransactionType.TOPUP, amount));
    }

    /**
     * Idempotent on (orderId, DEBIT) via ON CONFLICT DO NOTHING — a retried
     * pay() call for the same order returns the original transaction
     * instead of double-charging (same shape as coupon-service's apply,
     * Stage 7). Insufficient balance publishes payment.failed and throws,
     * mapped to 402 by GlobalExceptionHandler — order-service treats that
     * as an expected, handled outcome, not a crash.
     */
    @Transactional
    public WalletTransaction pay(UUID userId, UUID orderId, BigDecimal amount) {
        Optional<WalletTransaction> existing = transactionRepository.findByOrderIdAndType(orderId, TransactionType.DEBIT);
        if (existing.isPresent()) {
            return existing.get();
        }

        int debited = walletRepository.tryDebit(userId, amount);
        if (debited == 0) {
            eventPublisher.publishFailed(orderId, userId, amount);
            throw new InsufficientBalanceException(userId);
        }

        UUID transactionId = UUID.randomUUID();
        int inserted = transactionRepository.insertIfAbsent(
                transactionId, userId, orderId, TransactionType.DEBIT.name(), amount, Instant.now());

        WalletTransaction transaction;
        if (inserted == 0) {
            // Lost a race against a concurrent pay() for the same order — undo our own debit, defer to the winner.
            walletRepository.credit(userId, amount);
            transaction = transactionRepository.findByOrderIdAndType(orderId, TransactionType.DEBIT)
                    .orElseThrow(() -> new WalletNotFoundException(userId));
        } else {
            transaction = transactionRepository.findById(transactionId).orElseThrow(() -> new WalletNotFoundException(userId));
        }

        eventPublisher.publishSuccess(orderId, userId, amount);
        return transaction;
    }

    /** Called directly by order-service's cancel endpoint for symmetry/testability, and by PlatformEventConsumer on order.cancelled for the architected event-driven path — both converge here, both idempotent. */
    @Transactional
    public Optional<WalletTransaction> refundIfPaid(UUID orderId, UUID userId, BigDecimal amount) {
        boolean wasPaid = transactionRepository.findByOrderIdAndType(orderId, TransactionType.DEBIT).isPresent();
        if (!wasPaid) {
            return Optional.empty();
        }

        Optional<WalletTransaction> existingRefund = transactionRepository.findByOrderIdAndType(orderId, TransactionType.CREDIT);
        if (existingRefund.isPresent()) {
            return existingRefund;
        }

        walletRepository.credit(userId, amount);
        UUID transactionId = UUID.randomUUID();
        int inserted = transactionRepository.insertIfAbsent(
                transactionId, userId, orderId, TransactionType.CREDIT.name(), amount, Instant.now());

        if (inserted == 0) {
            // Lost a race against a concurrent refund for the same order — undo our own credit, defer to the winner.
            walletRepository.tryDebit(userId, amount);
            return transactionRepository.findByOrderIdAndType(orderId, TransactionType.CREDIT);
        }
        return transactionRepository.findById(transactionId);
    }
}
