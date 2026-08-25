package com.vicinia.inventoryservice.repository;

import com.vicinia.inventoryservice.domain.MerchantListing;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stage 17's "reservation math" integration test — the atomic
 * conditional-UPDATE guard behind ADR 0002's oversell prevention is real
 * SQL, not pure Java, so proving it correct needs a genuine Postgres
 * engine (not H2's approximation) actually evaluating the WHERE clause
 * under real row-level locking. @DataJpaTest defaults to an embedded DB —
 * @AutoConfigureTestDatabase(NONE) + @ServiceConnection on a real
 * Testcontainers Postgres overrides that.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class MerchantListingRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @org.springframework.beans.factory.annotation.Autowired
    private MerchantListingRepository listingRepository;

    private MerchantListing newListing(int availableStock) {
        MerchantListing listing = new MerchantListing(
                UUID.randomUUID(), "product-" + UUID.randomUUID(), "Test Product", "Test Category",
                new BigDecimal("10.00"), availableStock);
        return listingRepository.saveAndFlush(listing);
    }

    @Test
    void tryReserve_decrementsAvailableAndIncrementsReserved_whenStockSufficient() {
        MerchantListing listing = newListing(10);

        int updated = listingRepository.tryReserve(listing.getId(), 4);

        assertThat(updated).isEqualTo(1);
        MerchantListing reloaded = listingRepository.findById(listing.getId()).orElseThrow();
        assertThat(reloaded.getAvailableStock()).isEqualTo(6);
        assertThat(reloaded.getReservedStock()).isEqualTo(4);
    }

    @Test
    void tryReserve_isANoOp_whenStockInsufficient() {
        MerchantListing listing = newListing(3);

        int updated = listingRepository.tryReserve(listing.getId(), 5);

        assertThat(updated).isEqualTo(0);
        MerchantListing reloaded = listingRepository.findById(listing.getId()).orElseThrow();
        assertThat(reloaded.getAvailableStock()).isEqualTo(3);
        assertThat(reloaded.getReservedStock()).isEqualTo(0);
    }

    @Test
    void tryConfirm_movesQuantityOutOfReservedStock() {
        MerchantListing listing = newListing(10);
        listingRepository.tryReserve(listing.getId(), 4);

        int updated = listingRepository.tryConfirm(listing.getId(), 4);

        assertThat(updated).isEqualTo(1);
        MerchantListing reloaded = listingRepository.findById(listing.getId()).orElseThrow();
        assertThat(reloaded.getAvailableStock()).isEqualTo(6);
        assertThat(reloaded.getReservedStock()).isEqualTo(0);
    }

    @Test
    void tryRelease_returnsQuantityToAvailableStock() {
        MerchantListing listing = newListing(10);
        listingRepository.tryReserve(listing.getId(), 4);

        int updated = listingRepository.tryRelease(listing.getId(), 4);

        assertThat(updated).isEqualTo(1);
        MerchantListing reloaded = listingRepository.findById(listing.getId()).orElseThrow();
        assertThat(reloaded.getAvailableStock()).isEqualTo(10);
        assertThat(reloaded.getReservedStock()).isEqualTo(0);
    }

    /**
     * The actual oversell canary this project's Stage 16 Grafana dashboard
     * watches for at runtime, proven here under real concurrency: 20
     * threads each try to reserve 1 unit against a listing that starts
     * with only 10 available. Postgres's row lock on the UPDATE (ADR
     * 0002) should serialize all 20 attempts against the same row, so
     * exactly 10 succeed and 10 correctly no-op — never 11+ successes,
     * which would mean stock went negative.
     */
    @Test
    void tryReserve_neverOversellsUnderConcurrency() throws InterruptedException {
        MerchantListing listing = newListing(10);
        int attempts = 20;

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger(0);

        for (int i = 0; i < attempts; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    if (listingRepository.tryReserve(listing.getId(), 1) == 1) {
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

        assertThat(successes.get()).isEqualTo(10);
        MerchantListing reloaded = listingRepository.findById(listing.getId()).orElseThrow();
        assertThat(reloaded.getAvailableStock()).isEqualTo(0);
        assertThat(reloaded.getReservedStock()).isEqualTo(10);
    }

    // Note: the (orderId, listingId)-keyed idempotency for re-reserving
    // (ARCHITECTURE.md §11) lives in InventoryService.reserve()'s own
    // existing-reservation check, one layer above these repository-level
    // atomic queries — this repository has no orderId at all, by design
    // (see Reservation, not MerchantListing, for that), so it isn't
    // covered by this class.

    @Test
    void findByProductIdAndActiveTrue_onlyReturnsActiveListings() {
        String productId = "shared-product-" + UUID.randomUUID();
        MerchantListing active = listingRepository.saveAndFlush(new MerchantListing(
                UUID.randomUUID(), productId, "Test Product", "Test Category", new BigDecimal("10.00"), 5));
        MerchantListing inactive = listingRepository.saveAndFlush(new MerchantListing(
                UUID.randomUUID(), productId, "Test Product", "Test Category", new BigDecimal("12.00"), 5));
        inactive.updateDetails(null, null, false);
        listingRepository.saveAndFlush(inactive);

        List<MerchantListing> results = listingRepository.findByProductIdAndActiveTrue(productId);

        assertThat(results).extracting(MerchantListing::getId).containsExactly(active.getId());
    }
}
