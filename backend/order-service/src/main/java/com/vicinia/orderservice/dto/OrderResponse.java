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
        Instant createdAt,
        Instant updatedAt,
        String razorpayOrderId,
        String razorpayKeyId
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
                order.getCreatedAt(), order.getUpdatedAt(),
                result.razorpayOrderId(), result.razorpayKeyId()
        );
    }
}
