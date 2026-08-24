package com.vicinia.couponservice.service;

import com.vicinia.couponservice.domain.Coupon;
import com.vicinia.couponservice.domain.CouponUsage;
import com.vicinia.couponservice.dto.ApplyCouponRequest;
import com.vicinia.couponservice.dto.CreateCouponRequest;
import com.vicinia.couponservice.dto.UpdateCouponRequest;
import com.vicinia.couponservice.exception.CouponAlreadyUsedByUserException;
import com.vicinia.couponservice.exception.CouponCodeAlreadyExistsException;
import com.vicinia.couponservice.exception.CouponNotActiveException;
import com.vicinia.couponservice.exception.CouponNotFoundException;
import com.vicinia.couponservice.exception.CouponUsageLimitExceededException;
import com.vicinia.couponservice.exception.MinOrderValueNotMetException;
import com.vicinia.couponservice.repository.CouponRepository;
import com.vicinia.couponservice.repository.CouponUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository usageRepository;

    public CouponService(CouponRepository couponRepository, CouponUsageRepository usageRepository) {
        this.couponRepository = couponRepository;
        this.usageRepository = usageRepository;
    }

    // --- admin ---

    @Transactional
    public Coupon create(CreateCouponRequest request) {
        String code = request.code().toUpperCase();
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new CouponCodeAlreadyExistsException(code);
        }
        Coupon coupon = new Coupon(code, request.description(), request.discountType(), request.discountValue(),
                request.maxDiscountAmount(), request.minOrderValue(), request.usageLimit(), request.perUserLimit(),
                request.validFrom(), request.validUntil());
        return couponRepository.save(coupon);
    }

    public List<Coupon> listAll() {
        return couponRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Coupon update(UUID id, UpdateCouponRequest request) {
        Coupon coupon = getById(id);
        coupon.applyUpdate(request.description(), request.discountValue(), request.maxDiscountAmount(),
                request.minOrderValue(), request.usageLimit(), request.perUserLimit(),
                request.validFrom(), request.validUntil(), request.active());
        return couponRepository.save(coupon);
    }

    public Coupon getById(UUID id) {
        return couponRepository.findById(id).orElseThrow(() -> new CouponNotFoundException(id.toString()));
    }

    // --- customer ---

    /** Read-only preview — no state change, no usage-count increment. */
    public PreviewResult validate(String code, UUID userId, BigDecimal orderValue) {
        Coupon coupon = getByCode(code);
        assertUsable(coupon, userId, orderValue);
        return new PreviewResult(coupon, coupon.computeDiscount(orderValue));
    }

    /**
     * Idempotent on (couponId, orderId) — re-applying the same order
     * returns the existing usage rather than counting twice (ARCHITECTURE.md
     * §11's pattern, same as Stage 5's reserve). The atomic increment
     * (tryIncrementUsage) is what makes the *global* usageLimit safe under
     * concurrency — the concurrency test this stage exists for. perUserLimit
     * is checked transactionally but not given the same hard atomic
     * guarantee: the scenario it protects against (the same user racing
     * themselves on two simultaneous applies of one coupon) is much lower
     * stakes than N different customers racing for one limited code, which
     * is what usageLimit actually guards.
     *
     * A real race the increment alone doesn't cover: two concurrent apply()
     * calls for the exact same (code, orderId) could both pass the
     * "not already applied" check before either commits, both succeed at
     * tryIncrementUsage, and then race on recording the usage row — see
     * CouponUsageRepository.insertIfAbsent for why that uses ON CONFLICT DO
     * NOTHING rather than a plain insert caught in Java. The loser here
     * detects it inserted nothing and compensates its own spurious
     * increment back, rather than leaking a duplicate-counted usage.
     */
    @Transactional
    public CouponUsage apply(UUID userId, ApplyCouponRequest request) {
        Coupon coupon = getByCode(request.code());

        Optional<CouponUsage> existing = usageRepository.findByCouponIdAndOrderId(coupon.getId(), request.orderId());
        if (existing.isPresent()) {
            return existing.get();
        }

        assertUsable(coupon, userId, request.orderValue());

        int updated = couponRepository.tryIncrementUsage(coupon.getId());
        if (updated == 0) {
            throw new CouponUsageLimitExceededException(coupon.getCode());
        }

        BigDecimal discount = coupon.computeDiscount(request.orderValue());
        UUID usageId = UUID.randomUUID();
        int inserted = usageRepository.insertIfAbsent(
                usageId, coupon.getId(), userId, request.orderId(), discount, Instant.now());

        if (inserted == 0) {
            couponRepository.tryDecrementUsage(coupon.getId());
            return usageRepository.findByCouponIdAndOrderId(coupon.getId(), request.orderId())
                    .orElseThrow(() -> new CouponNotFoundException(request.code()));
        }

        return usageRepository.findById(usageId).orElseThrow(() -> new CouponNotFoundException(request.code()));
    }

    private Coupon getByCode(String code) {
        return couponRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new CouponNotFoundException(code));
    }

    private void assertUsable(Coupon coupon, UUID userId, BigDecimal orderValue) {
        if (!coupon.isCurrentlyValid(Instant.now())) {
            throw new CouponNotActiveException(coupon.getCode());
        }
        if (orderValue.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new MinOrderValueNotMetException(coupon.getCode(), coupon.getMinOrderValue());
        }
        if (coupon.getPerUserLimit() != null) {
            long usedByUser = usageRepository.countByCouponIdAndUserId(coupon.getId(), userId);
            if (usedByUser >= coupon.getPerUserLimit()) {
                throw new CouponAlreadyUsedByUserException(coupon.getCode());
            }
        }
    }

    public record PreviewResult(Coupon coupon, BigDecimal discountAmount) {
    }
}
