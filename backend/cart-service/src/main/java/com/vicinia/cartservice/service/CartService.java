package com.vicinia.cartservice.service;

import com.vicinia.cartservice.client.InventoryClient;
import com.vicinia.cartservice.domain.Cart;
import com.vicinia.cartservice.dto.AddItemRequest;
import com.vicinia.cartservice.dto.CartItemResponse;
import com.vicinia.cartservice.dto.CartResponse;
import com.vicinia.cartservice.exception.DifferentMerchantException;
import com.vicinia.cartservice.exception.ListingNotFoundException;
import com.vicinia.cartservice.repository.CartRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final InventoryClient inventoryClient;

    public CartService(CartRepository cartRepository, InventoryClient inventoryClient) {
        this.cartRepository = cartRepository;
        this.inventoryClient = inventoryClient;
    }

    public CartResponse getCart(UUID userId) {
        return enrich(cartRepository.findOrCreate(userId));
    }

    /**
     * ADR 0001 — one merchant per cart. Adding an item from a different
     * merchant than the cart is already pinned to is rejected (409), not
     * silently swapped; the client clears the cart first if that's really
     * what the customer wants. Stock sufficiency is deliberately NOT
     * checked here — only that the listing exists and is active. Whether
     * there's enough stock right now is the live check in enrich(), the
     * single source of truth for "is this actually purchasable," not
     * duplicated at write time too.
     */
    public CartResponse addItem(UUID userId, AddItemRequest request) {
        InventoryClient.ListingRef listing = inventoryClient.fetch(request.listingId())
                .filter(InventoryClient.ListingRef::active)
                .orElseThrow(() -> new ListingNotFoundException(request.listingId()));

        Cart cart = cartRepository.findOrCreate(userId);
        if (!cart.isEmpty() && !cart.getMerchantId().equals(listing.merchantId())) {
            throw new DifferentMerchantException();
        }

        cart.setMerchantId(listing.merchantId());
        cart.addOrIncrement(request.listingId(), request.quantity());
        cartRepository.save(cart);
        return enrich(cart);
    }

    public CartResponse updateItem(UUID userId, UUID listingId, int quantity) {
        Cart cart = cartRepository.findOrCreate(userId);
        cart.updateQuantity(listingId, quantity);
        cartRepository.save(cart);
        return enrich(cart);
    }

    public CartResponse removeItem(UUID userId, UUID listingId) {
        Cart cart = cartRepository.findOrCreate(userId);
        cart.removeItem(listingId);
        cartRepository.save(cart);
        return enrich(cart);
    }

    public void clear(UUID userId) {
        cartRepository.delete(userId);
    }

    /** The live price/availability check this stage exists for — every line is re-priced against inventory-service's current state, not a stored snapshot. */
    private CartResponse enrich(Cart cart) {
        List<CartItemResponse> lines = cart.getItems().stream()
                .map(item -> CartItemResponse.from(item, inventoryClient.fetch(item.getListingId()).orElse(null)))
                .toList();

        BigDecimal subtotal = lines.stream()
                .filter(CartItemResponse::available)
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getUserId(), cart.getMerchantId(), lines, subtotal);
    }
}
