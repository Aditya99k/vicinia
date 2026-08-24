package com.vicinia.cartservice.domain;

import com.vicinia.cartservice.exception.ItemNotInCartException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The whole cart is stored as one JSON blob under cart:{userId} in Redis
 * (ARCHITECTURE.md §12) — no relational storage at all; this is the one
 * service in the system with zero durability beyond a session (§6).
 * merchantId is null exactly when the cart is empty — ADR 0001 requires
 * every non-empty cart to pin to a single merchant, enforced in
 * CartService.addItem, not here (this class only tracks state, it doesn't
 * know what a "different merchant" error should look like as an HTTP
 * response).
 */
public class Cart {

    private UUID userId;
    private UUID merchantId;
    private List<CartItem> items = new ArrayList<>();
    private Instant updatedAt = Instant.now();

    protected Cart() {
    }

    public Cart(UUID userId) {
        this.userId = userId;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void addOrIncrement(UUID listingId, int quantity) {
        findItem(listingId).ifPresentOrElse(
                item -> item.setQuantity(item.getQuantity() + quantity),
                () -> items.add(new CartItem(listingId, quantity))
        );
        touch();
    }

    public void updateQuantity(UUID listingId, int quantity) {
        CartItem item = findItem(listingId).orElseThrow(() -> new ItemNotInCartException(listingId));
        item.setQuantity(quantity);
        touch();
    }

    public void removeItem(UUID listingId) {
        boolean removed = items.removeIf(item -> item.getListingId().equals(listingId));
        if (!removed) {
            throw new ItemNotInCartException(listingId);
        }
        if (items.isEmpty()) {
            merchantId = null;
        }
        touch();
    }

    private Optional<CartItem> findItem(UUID listingId) {
        return items.stream().filter(item -> item.getListingId().equals(listingId)).findFirst();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    // --- getters/setters ---

    public UUID getUserId() {
        return userId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
