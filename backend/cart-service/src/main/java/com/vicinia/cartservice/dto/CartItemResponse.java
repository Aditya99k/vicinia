package com.vicinia.cartservice.dto;

import com.vicinia.cartservice.client.InventoryClient;
import com.vicinia.cartservice.domain.CartItem;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * available is the live check this whole stage exists for: false either
 * because the listing was deactivated/deleted since it was added (listing
 * is null), or because its current stock has fallen below what's in the
 * cart. lineTotal is null whenever available is false — an unavailable
 * line can't be priced meaningfully, and CartService excludes it from the
 * cart's subtotal.
 */
public record CartItemResponse(
        UUID listingId,
        String productId,
        String productName,
        UUID merchantId,
        BigDecimal price,
        int quantity,
        int availableStock,
        boolean available,
        BigDecimal lineTotal
) {
    public static CartItemResponse from(CartItem item, InventoryClient.ListingRef listing) {
        boolean available = listing != null && listing.active() && listing.availableStock() >= item.getQuantity();
        BigDecimal lineTotal = available ? listing.price().multiply(BigDecimal.valueOf(item.getQuantity())) : null;

        return new CartItemResponse(
                item.getListingId(),
                listing != null ? listing.productId() : null,
                listing != null ? listing.productName() : "Listing no longer available",
                listing != null ? listing.merchantId() : null,
                listing != null ? listing.price() : null,
                item.getQuantity(),
                listing != null ? listing.availableStock() : 0,
                available,
                lineTotal
        );
    }
}
