package com.vicinia.inventoryservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.inventoryservice.domain.MerchantListing;
import com.vicinia.inventoryservice.dto.CreateListingRequest;
import com.vicinia.inventoryservice.dto.ListingResponse;
import com.vicinia.inventoryservice.dto.UpdateListingRequest;
import com.vicinia.inventoryservice.service.InventoryService;
import com.vicinia.inventoryservice.util.PermissionUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Merchant-facing listing management, gated by LISTING_MANAGE (already
 * seeded onto the MERCHANT role in auth-service's RoleSeeder — Stage 2).
 * merchantId is always the caller's own X-User-Id, never a client-supplied
 * value — see MerchantListing's class comment for why that correlation is
 * safe. /product/{productId} and /{id} are both public, unauthenticated
 * reads — the former for customer browsing (api-gateway's public-paths,
 * matching catalog-service's /products/search and /categories precedent),
 * the latter only ever called service-to-service (e.g. cart-service's
 * InventoryClient — Stage 6), so it just needs InternalRequestFilter to pass.
 */
@RestController
@RequestMapping("/api/inventory/listings")
public class ListingController {

    private static final String REQUIRED_PERMISSION = "LISTING_MANAGE";

    private final InventoryService inventoryService;

    public ListingController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<ListingResponse> create(@RequestHeader(HeaderNames.USER_ID) String userId,
                                                    @RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions,
                                                    @Valid @RequestBody CreateListingRequest request) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        MerchantListing listing = inventoryService.createListing(UUID.fromString(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ListingResponse.from(listing));
    }

    @GetMapping("/mine")
    public List<ListingResponse> mine(@RequestHeader(HeaderNames.USER_ID) String userId,
                                       @RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return inventoryService.myListings(UUID.fromString(userId)).stream().map(ListingResponse::from).toList();
    }

    @PutMapping("/{id}")
    public ListingResponse update(@RequestHeader(HeaderNames.USER_ID) String userId,
                                   @RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions,
                                   @PathVariable UUID id,
                                   @Valid @RequestBody UpdateListingRequest request) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return ListingResponse.from(inventoryService.updateListing(UUID.fromString(userId), id, request));
    }

    @GetMapping("/product/{productId}")
    public List<ListingResponse> byProduct(@PathVariable String productId) {
        return inventoryService.byProduct(productId).stream().map(ListingResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ListingResponse getById(@PathVariable UUID id) {
        return ListingResponse.from(inventoryService.getById(id));
    }
}
