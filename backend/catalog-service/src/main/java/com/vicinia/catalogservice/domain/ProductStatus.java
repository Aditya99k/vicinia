package com.vicinia.catalogservice.domain;

/**
 * The moderation gate (ARCHITECTURE.md §7): a merchant requesting a brand
 * new product starts PENDING_REVIEW and isn't searchable until an admin
 * approves it — prevents duplicate/junk products from polluting the global
 * catalog. Admin-authored products skip straight to APPROVED.
 */
public enum ProductStatus {
    PENDING_REVIEW,
    APPROVED,
    REJECTED
}
