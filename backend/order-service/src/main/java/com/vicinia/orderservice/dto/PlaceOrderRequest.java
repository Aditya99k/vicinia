package com.vicinia.orderservice.dto;

/**
 * couponCode is optional — a plain checkout with no coupon is the common
 * case. paymentMethod defaults to WALLET when omitted, preserving Stage
 * 8's original synchronous behavior unchanged. deliveryLatitude/Longitude
 * come straight from whichever address the customer already selected in
 * checkout (user-service's own AddressResponse) — the frontend sends them
 * along rather than order-service calling back to user-service to look
 * them up, same "already have it, don't add a hop" reasoning as cart's
 * own line-item snapshot. Both null for an address with no coordinates on
 * file (e.g. one added before this feature, or a customer who declined
 * location access).
 */
public record PlaceOrderRequest(
        String couponCode,
        PaymentMethod paymentMethod,
        Double deliveryLatitude,
        Double deliveryLongitude
) {
}
