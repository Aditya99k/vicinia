package com.vicinia.merchantservice.dto;

import com.vicinia.merchantservice.domain.Merchant;
import com.vicinia.merchantservice.domain.MerchantStatus;

/**
 * A store's live open/closed state for a customer already looking at it —
 * separate from MerchantSummaryResponse (GET /nearby), which only ever
 * lists LIVE merchants in the first place and so has never needed a status
 * field. This exists because /nearby's result is cached client-side
 * (useMerchantDirectory) for the whole session: a merchant that goes
 * TEMP_CLOSED/SUSPENDED after that cache is populated stays in it, so a
 * customer already on that store's page needs a fresh, authoritative check
 * before adding anything to a cart.
 */
public record MerchantStatusResponse(String storeName, boolean open, String status) {
    public static MerchantStatusResponse from(Merchant m) {
        return new MerchantStatusResponse(m.getStoreName(), m.getStatus() == MerchantStatus.LIVE, m.getStatus().name());
    }
}
