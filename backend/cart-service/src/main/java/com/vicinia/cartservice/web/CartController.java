package com.vicinia.cartservice.web;

import com.vicinia.cartservice.dto.AddItemRequest;
import com.vicinia.cartservice.dto.CartResponse;
import com.vicinia.cartservice.dto.UpdateItemRequest;
import com.vicinia.cartservice.service.CartService;
import com.vicinia.common.security.HeaderNames;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Every endpoint here is a plain authenticated user managing their own cart — no permission gate beyond a valid JWT, nothing here is public. */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse get(@RequestHeader(HeaderNames.USER_ID) String userId) {
        return cartService.getCart(UUID.fromString(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@RequestHeader(HeaderNames.USER_ID) String userId,
                                                 @Valid @RequestBody AddItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addItem(UUID.fromString(userId), request));
    }

    @PutMapping("/items/{listingId}")
    public CartResponse updateItem(@RequestHeader(HeaderNames.USER_ID) String userId,
                                    @PathVariable UUID listingId,
                                    @Valid @RequestBody UpdateItemRequest request) {
        return cartService.updateItem(UUID.fromString(userId), listingId, request.quantity());
    }

    @DeleteMapping("/items/{listingId}")
    public CartResponse removeItem(@RequestHeader(HeaderNames.USER_ID) String userId,
                                    @PathVariable UUID listingId) {
        return cartService.removeItem(UUID.fromString(userId), listingId);
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(@RequestHeader(HeaderNames.USER_ID) String userId) {
        cartService.clear(UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }
}
