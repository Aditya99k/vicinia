package com.vicinia.paymentservice.repository;

import com.vicinia.paymentservice.domain.TransactionType;
import com.vicinia.paymentservice.domain.Wallet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stage 17 — proves WalletService.pay()'s idempotency claim (ARCHITECTURE.md
 * §11: "unique constraint: one successful transaction per orderId") against
 * a real Postgres, the same way MerchantListingRepositoryIntegrationTest
 * proves inventory-service's reservation math. tryDebit's atomic
 * conditional UPDATE prevents an over-debit; insertIfAbsent's real
 * ON CONFLICT (order_id, type) DO NOTHING is what actually prevents two
 * concurrent pay() calls for the same order from both succeeding.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class WalletRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Test
    void tryDebit_decrementsBalance_whenSufficientFunds() {
        UUID userId = UUID.randomUUID();
        creditedWallet(userId, "100.00");

        int updated = walletRepository.tryDebit(userId, new BigDecimal("40.00"));

        assertThat(updated).isEqualTo(1);
        assertThat(walletRepository.findByUserId(userId).orElseThrow().getBalance())
                .isEqualByComparingTo("60.00");
    }

    @Test
    void tryDebit_isANoOp_whenInsufficientFunds() {
        UUID userId = UUID.randomUUID();
        creditedWallet(userId, "10.00");

        int updated = walletRepository.tryDebit(userId, new BigDecimal("40.00"));

        assertThat(updated).isEqualTo(0);
        assertThat(walletRepository.findByUserId(userId).orElseThrow().getBalance())
                .isEqualByComparingTo("10.00");
    }

    @Test
    void insertIfAbsent_recordsOnlyOneDebitTransaction_forTheSameOrder() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        int firstInsert = transactionRepository.insertIfAbsent(
                UUID.randomUUID(), userId, orderId, TransactionType.DEBIT.name(), new BigDecimal("40.00"), Instant.now());
        int secondInsert = transactionRepository.insertIfAbsent(
                UUID.randomUUID(), userId, orderId, TransactionType.DEBIT.name(), new BigDecimal("40.00"), Instant.now());

        assertThat(firstInsert).isEqualTo(1);
        assertThat(secondInsert).isEqualTo(0);
        assertThat(transactionRepository.findByOrderIdAndType(orderId, TransactionType.DEBIT)).isPresent();
    }

    /**
     * The real concurrency proof: 10 threads all call pay() for the exact
     * same order simultaneously. Only one should durably debit the wallet
     * and record a transaction — the ON CONFLICT DO NOTHING is what makes
     * the "losing" callers safe to just credit back their own debit (see
     * WalletService.pay()'s own comment on this), not this repository
     * test's job to simulate — this test only proves the DB-level
     * primitive that makes that possible actually holds under real
     * concurrent access.
     */
    @Test
    void onlyOneConcurrentInsertIfAbsentCall_succeeds_forTheSameOrder() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        int attempts = 10;

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger(0);

        for (int i = 0; i < attempts; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    int result = transactionRepository.insertIfAbsent(
                            UUID.randomUUID(), userId, orderId, TransactionType.DEBIT.name(), new BigDecimal("40.00"), Instant.now());
                    if (result == 1) {
                        successes.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await();
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(successes.get()).isEqualTo(1);
    }

    /** credit() is a raw @Modifying UPDATE, not reflected on the in-memory Wallet object — nothing here should return or reuse that stale reference. */
    private void creditedWallet(UUID userId, String amount) {
        walletRepository.saveAndFlush(new Wallet(userId));
        walletRepository.credit(userId, new BigDecimal(amount));
    }
}
