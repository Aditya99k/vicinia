package com.vicinia.orderservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.orderservice.dto.CancelOrderRequest;
import com.vicinia.orderservice.dto.OrderResponse;
import com.vicinia.orderservice.dto.PlaceOrderRequest;
import com.vicinia.orderservice.dto.PlaceOrderResult;
import com.vicinia.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Customer-facing, gateway-routed — every endpoint scoped to the caller's own orders (X-User-Id), no admin surface in this stage. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(@RequestHeader(HeaderNames.USER_ID) String userId,
                                                @RequestBody PlaceOrderRequest request) {
        PlaceOrderResult result = orderService.placeOrder(UUID.fromString(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(result));
    }

    @GetMapping("/mine")
    public List<OrderResponse> mine(@RequestHeader(HeaderNames.USER_ID) String userId) {
        return orderService.myOrders(UUID.fromString(userId)).stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    public OrderResponse getById(@RequestHeader(HeaderNames.USER_ID) String userId, @PathVariable UUID id) {
        return OrderResponse.from(orderService.getById(id, UUID.fromString(userId)));
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@RequestHeader(HeaderNames.USER_ID) String userId,
                                 @PathVariable UUID id,
                                 @Valid @RequestBody CancelOrderRequest request) {
        return OrderResponse.from(orderService.cancel(id, UUID.fromString(userId), request.reason()));
    }
}
