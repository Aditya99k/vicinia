package com.vicinia.inventoryservice.web;

import com.vicinia.inventoryservice.domain.Reservation;
import com.vicinia.inventoryservice.dto.OrderIdRequest;
import com.vicinia.inventoryservice.dto.ReservationResponse;
import com.vicinia.inventoryservice.dto.ReserveRequest;
import com.vicinia.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal-only, by design: no user permission check here, because the real
 * caller is never a person — it's order-service (ARCHITECTURE.md §8:
 * "order-service -> inventory-service, REST, reservation must succeed/fail
 * before order creation"). Until Stage 8 builds that caller, InternalRequestFilter
 * (checked on every request regardless of path) is the only gate: a caller
 * needs X-Internal-Secret, not a JWT, matching exactly what order-service
 * will send later. Test directly against this service's own port, not
 * through api-gateway — the gateway's blanket /api/inventory/** route isn't
 * in public-paths, so it would demand a JWT these calls were never meant to carry.
 */
@RestController
@RequestMapping("/api/inventory/reservations")
public class ReservationController {

    private final InventoryService inventoryService;

    public ReservationController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/reserve")
    public ResponseEntity<List<ReservationResponse>> reserve(@Valid @RequestBody ReserveRequest request) {
        List<Reservation> reservations = inventoryService.reserve(request.orderId(), request.items());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservations.stream().map(ReservationResponse::from).toList());
    }

    @PostMapping("/confirm")
    public List<ReservationResponse> confirm(@Valid @RequestBody OrderIdRequest request) {
        return inventoryService.confirm(request.orderId()).stream().map(ReservationResponse::from).toList();
    }

    @PostMapping("/release")
    public List<ReservationResponse> release(@Valid @RequestBody OrderIdRequest request) {
        return inventoryService.release(request.orderId()).stream().map(ReservationResponse::from).toList();
    }
}
