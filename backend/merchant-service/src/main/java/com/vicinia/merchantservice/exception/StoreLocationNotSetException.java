package com.vicinia.merchantservice.exception;

/** delivery-service needs a lat/lng to search for a nearby partner around — see MerchantOrderService.markReady. */
public class StoreLocationNotSetException extends RuntimeException {
    public StoreLocationNotSetException() {
        super("Set your store's location before marking an order ready for pickup");
    }
}
