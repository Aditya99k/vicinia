package com.vicinia.cartservice.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID userId,
        UUID merchantId,
        List<CartItemResponse> items,
        BigDecimal subtotal
) {
}
