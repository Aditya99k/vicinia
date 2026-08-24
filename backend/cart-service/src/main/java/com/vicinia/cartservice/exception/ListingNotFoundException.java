package com.vicinia.cartservice.exception;

import java.util.UUID;

public class ListingNotFoundException extends RuntimeException {
    public ListingNotFoundException(UUID listingId) {
        super("Listing not found or no longer active: " + listingId);
    }
}
