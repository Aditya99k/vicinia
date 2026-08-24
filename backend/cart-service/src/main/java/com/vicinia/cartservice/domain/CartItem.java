package com.vicinia.cartservice.domain;

import java.util.UUID;

public class CartItem {

    private UUID listingId;
    private int quantity;

    protected CartItem() {
    }

    public CartItem(UUID listingId, int quantity) {
        this.listingId = listingId;
        this.quantity = quantity;
    }

    public UUID getListingId() {
        return listingId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
