package com.vicinia.merchantservice.service;

import com.vicinia.merchantservice.domain.Merchant;
import com.vicinia.merchantservice.domain.MerchantDocument;
import com.vicinia.merchantservice.domain.MerchantStatus;
import com.vicinia.merchantservice.dto.ApplyRequest;
import com.vicinia.merchantservice.dto.UpdateHoursRequest;
import com.vicinia.merchantservice.dto.UpdateStoreProfileRequest;
import com.vicinia.merchantservice.exception.MerchantAlreadyExistsException;
import com.vicinia.merchantservice.exception.MerchantNotFoundException;
import com.vicinia.merchantservice.exception.OnboardingIncompleteException;
import com.vicinia.merchantservice.messaging.MerchantEventPublisher;
import com.vicinia.merchantservice.repository.MerchantRepository;
import com.vicinia.merchantservice.util.GeoDistance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final MerchantEventPublisher eventPublisher;

    public MerchantService(MerchantRepository merchantRepository, MerchantEventPublisher eventPublisher) {
        this.merchantRepository = merchantRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Merchant apply(UUID ownerUserId, ApplyRequest request) {
        if (merchantRepository.existsByOwnerUserId(ownerUserId)) {
            throw new MerchantAlreadyExistsException();
        }

        Merchant merchant = new Merchant(
                ownerUserId, request.storeName(), request.description(),
                request.addressLine1(), request.city(), request.state(), request.pincode()
        );
        request.documents().forEach(doc ->
                merchant.addDocument(new MerchantDocument(doc.documentType(), doc.referenceUrl())));

        return merchantRepository.save(merchant);
    }

    public Merchant getMine(UUID ownerUserId) {
        return getByOwnerUserId(ownerUserId);
    }

    public Merchant getByOwnerUserId(UUID ownerUserId) {
        return merchantRepository.findByOwnerUserId(ownerUserId)
                .orElseThrow(MerchantNotFoundException::new);
    }

    public Merchant getById(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId.toString()));
    }

    @Transactional
    public Merchant updateProfile(UUID ownerUserId, UpdateStoreProfileRequest request) {
        Merchant merchant = getMine(ownerUserId);
        merchant.updateProfile(
                request.storeName(), request.description(), request.addressLine1(),
                request.city(), request.state(), request.pincode(),
                request.latitude(), request.longitude(), request.deliveryRadiusKm()
        );
        return merchantRepository.save(merchant);
    }

    @Transactional
    public Merchant updateHours(UUID ownerUserId, UpdateHoursRequest request) {
        Merchant merchant = getMine(ownerUserId);
        merchant.updateHours(request.openTime(), request.closeTime());
        return merchantRepository.save(merchant);
    }

    /**
     * Merchant-initiated ONBOARDING -> LIVE. ARCHITECTURE.md §4.1 also gates
     * this on having at least one catalog listing, which can't be enforced
     * yet — catalog-service doesn't exist until Stage 4. Gating on business
     * hours being set is the honest subset of that check available today;
     * revisit once catalog-service ships.
     */
    @Transactional
    public Merchant goLive(UUID ownerUserId) {
        Merchant merchant = getMine(ownerUserId);
        if (!merchant.hasHoursSet()) {
            throw new OnboardingIncompleteException("Set your business hours before going live");
        }
        merchant.transitionTo(MerchantStatus.LIVE);
        return merchantRepository.save(merchant);
    }

    @Transactional
    public Merchant close(UUID ownerUserId) {
        Merchant merchant = getMine(ownerUserId);
        merchant.transitionTo(MerchantStatus.TEMP_CLOSED);
        return merchantRepository.save(merchant);
    }

    @Transactional
    public Merchant reopen(UUID ownerUserId) {
        Merchant merchant = getMine(ownerUserId);
        merchant.transitionTo(MerchantStatus.LIVE);
        return merchantRepository.save(merchant);
    }

    /**
     * lat/lng, when given, take over entirely: every LIVE merchant with its
     * own coordinates on file is distance-checked against the customer's
     * position and kept only if within that merchant's own deliveryRadiusKm
     * (a merchant advertising "I deliver within 5km" shouldn't show up for
     * a customer 8km away, however far the customer's own search radius
     * might reach), sorted nearest-first. City matching is the fallback for
     * an address with no coordinates yet — increasingly rare now that
     * AddressFormModal requires them, but old addresses can still predate
     * that requirement.
     *
     * This also happens to be the real fix for a genuine bug city matching
     * had no way to avoid: "Bangalore" and "Bengaluru" are the same city
     * typed two different ways, and exact (even case-insensitive) string
     * matching treats them as different places — a merchant registered
     * under one spelling was simply invisible to a customer whose address
     * used the other. Distance-based matching sidesteps the whole problem;
     * it never looks at the city string at all.
     */
    public List<Merchant> nearby(String city, Double latitude, Double longitude) {
        List<Merchant> live = merchantRepository.findByStatus(MerchantStatus.LIVE);

        if (latitude != null && longitude != null) {
            return live.stream()
                    .filter(m -> m.getLatitude() != null && m.getLongitude() != null)
                    .filter(m -> GeoDistance.km(latitude, longitude, m.getLatitude(), m.getLongitude()) <= m.getDeliveryRadiusKm())
                    .sorted(Comparator.comparingDouble(m -> GeoDistance.km(latitude, longitude, m.getLatitude(), m.getLongitude())))
                    .toList();
        }

        return (city == null || city.isBlank())
                ? live
                : merchantRepository.findByStatusAndCityIgnoreCase(MerchantStatus.LIVE, city);
    }

    // --- admin ---

    public List<Merchant> pendingReview() {
        return merchantRepository.findByStatusOrderByCreatedAtAsc(MerchantStatus.PENDING_REVIEW);
    }

    /** Every merchant regardless of status, newest first — the admin's record of everyone it has ever reviewed, not just the still-pending queue. */
    public List<Merchant> all() {
        return merchantRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * PENDING_REVIEW -> APPROVED -> ONBOARDING in one call: the event fires
     * at APPROVED (ARCHITECTURE.md §10 workflow B), then the service
     * self-transitions to ONBOARDING immediately after — there's no
     * separate consumer-driven step in between, it's the same service.
     */
    @Transactional
    public Merchant approve(UUID merchantId) {
        Merchant merchant = getById(merchantId);
        merchant.transitionTo(MerchantStatus.APPROVED);
        eventPublisher.publishApproved(merchant.getId().toString());
        merchant.transitionTo(MerchantStatus.ONBOARDING);
        return merchantRepository.save(merchant);
    }

    @Transactional
    public Merchant reject(UUID merchantId, String reason) {
        Merchant merchant = getById(merchantId);
        merchant.transitionTo(MerchantStatus.REJECTED);
        merchant.setRejectionReason(reason);
        return merchantRepository.save(merchant);
    }

    @Transactional
    public Merchant suspend(UUID merchantId, String reason) {
        Merchant merchant = getById(merchantId);
        merchant.transitionTo(MerchantStatus.SUSPENDED);
        merchant.setSuspensionReason(reason);
        merchantRepository.save(merchant);
        eventPublisher.publishSuspended(merchant.getId().toString(), reason);
        return merchant;
    }

    @Transactional
    public Merchant reinstate(UUID merchantId) {
        Merchant merchant = getById(merchantId);
        merchant.transitionTo(MerchantStatus.LIVE);
        return merchantRepository.save(merchant);
    }
}
