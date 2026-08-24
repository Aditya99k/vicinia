package com.vicinia.orderservice.dto;

import com.vicinia.orderservice.domain.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID listingId,
        String productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getListingId(), item.getProductId(), item.getProductName(),
                item.getUnitPrice(), item.getQuantity(), item.getLineTotal()
        );
    }
}
