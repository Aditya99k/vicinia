package com.vicinia.orderservice.dto;

import com.vicinia.orderservice.domain.Order;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The delivery partner's slim view of an order — just enough to know what
 * (if anything) to collect on handover, not the full item/address detail
 * OrderResponse carries. Deliberately not ownership-checked against a
 * specific assigned partner (order-service has no notion of who that is —
 * that's delivery-service's own state); gated by the DELIVERY_MANAGE
 * permission instead, the same "narrow by role, not by an unreachable
 * cross-service ownership check" tradeoff this project already makes
 * elsewhere (see ListingController's /{id}).
 */
public record OrderDeliveryViewResponse(
        UUID orderId,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        boolean paid
) {
    public static OrderDeliveryViewResponse from(Order order) {
        return new OrderDeliveryViewResponse(order.getId(), order.getTotalAmount(), order.getPaymentMethod(), order.isPaid());
    }
}
