package com.vicinia.orderservice.web;

import com.vicinia.orderservice.dto.HasDeliveredResponse;
import com.vicinia.orderservice.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Internal-only, matching inventory-service's ReservationController /
 * payment-service's InternalPaymentController pattern exactly: no user
 * permission check, because the real caller is never a person — it's
 * review-service, verifying a user actually received a delivered order
 * for a product before letting them review it (Stage 13).
 * InternalRequestFilter (checked on every request regardless of path) is
 * the only gate: a caller needs X-Internal-Secret, not a JWT.
 */
@RestController
@RequestMapping("/api/orders/internal")
public class InternalOrderController {

    private final OrderService orderService;

    public InternalOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/delivered")
    public HasDeliveredResponse delivered(@RequestParam UUID userId, @RequestParam String productId) {
        return new HasDeliveredResponse(orderService.hasDeliveredProduct(userId, productId));
    }
}
