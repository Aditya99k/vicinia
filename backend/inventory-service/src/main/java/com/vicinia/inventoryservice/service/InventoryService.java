package com.vicinia.inventoryservice.service;

import com.vicinia.inventoryservice.client.CatalogClient;
import com.vicinia.inventoryservice.domain.MerchantListing;
import com.vicinia.inventoryservice.domain.Reservation;
import com.vicinia.inventoryservice.domain.ReservationStatus;
import com.vicinia.inventoryservice.dto.CreateListingRequest;
import com.vicinia.inventoryservice.dto.ReserveItemRequest;
import com.vicinia.inventoryservice.dto.UpdateListingRequest;
import com.vicinia.inventoryservice.exception.ForbiddenException;
import com.vicinia.inventoryservice.exception.InsufficientStockException;
import com.vicinia.inventoryservice.exception.ListingAlreadyExistsException;
import com.vicinia.inventoryservice.exception.ListingNotFoundException;
import com.vicinia.inventoryservice.exception.UnknownProductException;
import com.vicinia.inventoryservice.messaging.InventoryEventPublisher;
import com.vicinia.inventoryservice.repository.KnownProductRepository;
import com.vicinia.inventoryservice.repository.MerchantListingRepository;
import com.vicinia.inventoryservice.repository.ReservationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryService {

    private final MerchantListingRepository listingRepository;
    private final ReservationRepository reservationRepository;
    private final KnownProductRepository knownProductRepository;
    private final CatalogClient catalogClient;
    private final InventoryEventPublisher eventPublisher;
    private final int lowStockThreshold;

    private final Counter oversellCounter;

    public InventoryService(MerchantListingRepository listingRepository,
                             ReservationRepository reservationRepository,
                             KnownProductRepository knownProductRepository,
                             CatalogClient catalogClient,
                             InventoryEventPublisher eventPublisher,
                             @Value("${vicinia.inventory.low-stock-threshold:5}") int lowStockThreshold,
                             MeterRegistry meterRegistry) {
        this.listingRepository = listingRepository;
        this.reservationRepository = reservationRepository;
        this.knownProductRepository = knownProductRepository;
        this.catalogClient = catalogClient;
        this.eventPublisher = eventPublisher;
        this.lowStockThreshold = lowStockThreshold;
        // Registered eagerly, not lazily on first increment (Micrometer's
        // default via meterRegistry.counter(...).increment()) — a canary
        // that's supposed to read "always zero" needs to actually report 0
        // from boot, not simply be absent from /actuator/prometheus until
        // the bug it's watching for happens once.
        this.oversellCounter = Counter.builder("inventory.oversell")
                .description("Reservations that left available_stock negative — should never increment; a nonzero reading means the oversell guard (ADR §4.4) has broken")
                .register(meterRegistry);
    }

    // --- listings ---

    @Transactional
    public MerchantListing createListing(UUID merchantId, CreateListingRequest request) {
        if (listingRepository.existsByMerchantIdAndProductId(merchantId, request.productId())) {
            throw new ListingAlreadyExistsException();
        }
        CatalogClient.ProductRef product = resolveProduct(request.productId());
        MerchantListing listing = new MerchantListing(
                merchantId, product.id(), product.name(), product.category(),
                request.price(), request.availableStock());
        return listingRepository.save(listing);
    }

    public List<MerchantListing> myListings(UUID merchantId) {
        return listingRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    public List<MerchantListing> byProduct(String productId) {
        return listingRepository.findByProductIdAndActiveTrue(productId);
    }

    /** A store's own public catalog — customer-facing (Stage 18's shop page), same active-only shape as byProduct. */
    public List<MerchantListing> byMerchant(UUID merchantId) {
        return listingRepository.findByMerchantIdAndActiveTrueOrderByCreatedAtDesc(merchantId);
    }

    @Transactional
    public MerchantListing updateListing(UUID merchantId, UUID listingId, UpdateListingRequest request) {
        MerchantListing listing = getById(listingId);
        if (!listing.getMerchantId().equals(merchantId)) {
            throw new ForbiddenException("You do not own this listing");
        }
        listing.updateDetails(request.price(), request.availableStock(), request.active());
        return listingRepository.save(listing);
    }

    public MerchantListing getById(UUID id) {
        return listingRepository.findById(id).orElseThrow(() -> new ListingNotFoundException(id));
    }

    /** Cache-first (KnownProduct, populated by ProductEventConsumer), REST fallback on a miss — see CatalogClient. */
    private CatalogClient.ProductRef resolveProduct(String productId) {
        return knownProductRepository.findById(productId)
                .map(kp -> new CatalogClient.ProductRef(kp.getId(), kp.getName(), kp.getCategory()))
                .or(() -> catalogClient.fetch(productId))
                .orElseThrow(() -> new UnknownProductException(productId));
    }

    // --- reservations (ADR 0002/0003) ---

    /**
     * Reserves every line item for one orderId. Re-reserving an
     * (orderId, listingId) pair already on file is a no-op (ARCHITECTURE.md
     * §11), not a double-decrement. If any line in this call fails for
     * insufficient stock, every line newly reserved earlier in this same
     * call is released before the 409 is thrown — a partial reservation
     * never survives a failed reserve request.
     */
    @Transactional
    public List<Reservation> reserve(UUID orderId, List<ReserveItemRequest> items) {
        List<Reservation> result = new ArrayList<>();
        List<Reservation> newlyCreated = new ArrayList<>();

        for (ReserveItemRequest item : items) {
            Optional<Reservation> existing = reservationRepository.findByOrderIdAndListingId(orderId, item.listingId());
            if (existing.isPresent()) {
                result.add(existing.get());
                continue;
            }

            int updated = listingRepository.tryReserve(item.listingId(), item.quantity());
            if (updated == 0) {
                newlyCreated.forEach(this::releaseOne);
                throw new InsufficientStockException(item.listingId());
            }

            Reservation reservation = reservationRepository.save(new Reservation(orderId, item.listingId(), item.quantity()));
            result.add(reservation);
            newlyCreated.add(reservation);
            checkStockThresholds(item.listingId());
        }

        return result;
    }

    @Transactional
    public List<Reservation> confirm(UUID orderId) {
        List<Reservation> pending = reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.PAYMENT_PENDING);
        for (Reservation reservation : pending) {
            listingRepository.tryConfirm(reservation.getListingId(), reservation.getQuantity());
            reservation.confirm();
        }
        return reservationRepository.saveAll(pending);
    }

    @Transactional
    public List<Reservation> release(UUID orderId) {
        List<Reservation> pending = reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.PAYMENT_PENDING);
        pending.forEach(this::releaseOne);
        return pending;
    }

    /** Shared by the reserve-rollback path, the explicit release-by-order endpoint, and ReservationReaper. */
    @Transactional
    public void releaseOne(Reservation reservation) {
        listingRepository.tryRelease(reservation.getListingId(), reservation.getQuantity());
        reservation.release();
        reservationRepository.save(reservation);
    }

    /**
     * KafkaTemplate.send isn't covered by the surrounding JPA transaction —
     * a multi-item reserve() that decrements this listing but then rolls
     * back the whole call because a *later* line fails would otherwise
     * still leave a real inventory.low/out event on the topic for a stock
     * change that never actually took effect. Deferring the publish to
     * afterCommit closes that gap: nothing is sent unless reserve()'s
     * transaction actually commits.
     */
    private void checkStockThresholds(UUID listingId) {
        MerchantListing listing = getById(listingId);
        String productId = listing.getProductId();
        UUID merchantId = listing.getMerchantId();
        int availableStock = listing.getAvailableStock();

        // Stage 16's inventory-oversell canary (ARCHITECTURE.md §15) — this
        // should be structurally impossible, since tryReserve's own atomic
        // conditional UPDATE (WHERE available_stock >= quantity) is what
        // ADR/§4.4 relies on to prevent oversell in the first place. A
        // non-zero reading here means that guarantee has actually broken,
        // not just that stock ran low — that's why this dashboard's "should
        // always be zero" framing matters: any nonzero count is a real bug,
        // not a routine business event like inventory.low/out below.
        if (availableStock < 0) {
            oversellCounter.increment();
        }

        if (availableStock == 0) {
            afterCommit(() -> eventPublisher.publishOut(productId, merchantId, listingId));
        } else if (availableStock <= lowStockThreshold) {
            afterCommit(() -> eventPublisher.publishLow(productId, merchantId, listingId, availableStock));
        }
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
