package com.vicinia.orderservice.dto;

import com.vicinia.orderservice.domain.Order;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The delivery partner's slim view of an order — just enough to know what
 * (if anything) to collect on handover, and where to actually take it, not
 * the full item/address detail OrderResponse carries. Deliberately not
 * ownership-checked against a specific assigned partner (order-service has
 * no notion of who that is — that's delivery-service's own state); gated
 * by the DELIVERY_MANAGE permission instead, the same "narrow by role, not
 * by an unreachable cross-service ownership check" tradeoff this project
 * already makes elsewhere (see ListingController's /{id}).
 *
 * customerName/customerPhone are a live cross-service lookup (RiderClient,
 * despite the name — its contactSummary method is just a generic
 * userId -> {fullName, phone} call, already used for the customer's own
 * "who's delivering this" lookup in the opposite direction), not a
 * snapshot: unlike the address text/coordinates, a phone number staying
 * current actually matters for a rider trying to reach someone right now.
 */
public record OrderDeliveryViewResponse(
        UUID orderId,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        boolean paid,
        Double dropoffLatitude,
        Double dropoffLongitude,
        String dropoffAddress,
        String customerName,
        String customerPhone
) {
    public static OrderDeliveryViewResponse from(Order order, String customerName, String customerPhone) {
        return new OrderDeliveryViewResponse(
                order.getId(), order.getTotalAmount(), order.getPaymentMethod(), order.isPaid(),
                order.getDeliveryLatitude(), order.getDeliveryLongitude(), order.getDeliveryAddressLine(),
                customerName, customerPhone
        );
    }
}
