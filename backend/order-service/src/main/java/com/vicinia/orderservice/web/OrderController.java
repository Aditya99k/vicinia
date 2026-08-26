package com.vicinia.orderservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.orderservice.client.RiderClient;
import com.vicinia.orderservice.dto.CancelOrderRequest;
import com.vicinia.orderservice.dto.OrderDeliveryViewResponse;
import com.vicinia.orderservice.dto.OrderResponse;
import com.vicinia.orderservice.dto.PlaceOrderRequest;
import com.vicinia.orderservice.dto.PlaceOrderResult;
import com.vicinia.orderservice.service.OrderService;
import com.vicinia.orderservice.util.PermissionUtil;
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

/** Customer-facing, gateway-routed — every endpoint scoped to the caller's own orders (X-User-Id), except /merchant-view which scopes by the caller as the owning merchant instead. No admin surface in this stage. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final RiderClient riderClient;

    public OrderController(OrderService orderService, RiderClient riderClient) {
        this.orderService = orderService;
        this.riderClient = riderClient;
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
        OrderResponse response = OrderResponse.from(orderService.getById(id, UUID.fromString(userId)));
        return riderClient.partnerUserIdFor(id)
                .flatMap(riderClient::contactSummary)
                .map(contact -> response.withRider(contact.fullName(), contact.phone()))
                .orElse(response);
    }

    /** The merchant's own view of one of their orders — full item detail, scoped by merchantId instead of customer userId. */
    @GetMapping("/{id}/merchant-view")
    public OrderResponse getByIdForMerchant(@RequestHeader(HeaderNames.USER_ID) String userId, @PathVariable UUID id) {
        return OrderResponse.from(orderService.getByIdForMerchant(id, UUID.fromString(userId)));
    }

    /** The delivery partner's slim view — payment amount/method/paid only, gated by the DELIVERY_MANAGE permission, not per-order ownership (see OrderDeliveryViewResponse). */
    @GetMapping("/{id}/delivery-view")
    public OrderDeliveryViewResponse getForDelivery(@RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions, @PathVariable UUID id) {
        PermissionUtil.require(permissions, "DELIVERY_MANAGE");
        return OrderDeliveryViewResponse.from(orderService.getForDelivery(id));
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@RequestHeader(HeaderNames.USER_ID) String userId,
                                 @PathVariable UUID id,
                                 @Valid @RequestBody CancelOrderRequest request) {
        return OrderResponse.from(orderService.cancel(id, UUID.fromString(userId), request.reason()));
    }
}
