package com.vicinia.orderservice.dto;

import com.vicinia.orderservice.domain.Order;
import com.vicinia.orderservice.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        UUID merchantId,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        String couponCode,
        BigDecimal totalAmount,
        String cancellationReason,
        PaymentMethod paymentMethod,
        boolean paid,
        Instant createdAt,
        Instant updatedAt,
        String razorpayOrderId,
        String razorpayKeyId,
        String riderName,
        String riderPhone
) {
    public static OrderResponse from(Order order) {
        return from(PlaceOrderResult.wallet(order));
    }

    public static OrderResponse from(PlaceOrderResult result) {
        Order order = result.order();
        return new OrderResponse(
                order.getId(), order.getUserId(), order.getMerchantId(), order.getStatus(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getSubtotal(), order.getDiscountAmount(), order.getCouponCode(),
                order.getTotalAmount(), order.getCancellationReason(),
                order.getPaymentMethod(), order.isPaid(),
                order.getCreatedAt(), order.getUpdatedAt(),
                result.razorpayOrderId(), result.razorpayKeyId(),
                null, null
        );
    }

    /** Copy-with for the one enrichment that needs a second, cross-service round trip (RiderClient) — deliberately not part of the base mapping above, which stays a pure, network-free projection of Order. */
    public OrderResponse withRider(String riderName, String riderPhone) {
        return new OrderResponse(
                id, userId, merchantId, status, items, subtotal, discountAmount, couponCode,
                totalAmount, cancellationReason, paymentMethod, paid, createdAt, updatedAt,
                razorpayOrderId, razorpayKeyId, riderName, riderPhone
        );
    }
}
