package com.vicinia.deliveryservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.deliveryservice.dto.LocationRequest;
import com.vicinia.deliveryservice.dto.PartnerResponse;
import com.vicinia.deliveryservice.service.DeliveryService;
import com.vicinia.deliveryservice.util.PermissionUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** A delivery partner managing their own online status and location — gated by DELIVERY_MANAGE (seeded onto the DELIVERY_PARTNER role since Stage 2's RoleSeeder). */
@RestController
@RequestMapping("/api/delivery/partners")
public class PartnerController {

    private static final String REQUIRED_PERMISSION = "DELIVERY_MANAGE";

    private final DeliveryService deliveryService;

    public PartnerController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/me")
    public PartnerResponse me(@RequestHeader(HeaderNames.USER_ID) String userId,
                               @RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return PartnerResponse.from(deliveryService.me(UUID.fromString(userId)));
    }

    @PostMapping("/online")
    public PartnerResponse goOnline(@RequestHeader(HeaderNames.USER_ID) String userId,
                                     @RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions,
                                     @Valid @RequestBody LocationRequest request) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return PartnerResponse.from(deliveryService.goOnline(UUID.fromString(userId), request.latitude(), request.longitude()));
    }

    @PostMapping("/offline")
    public PartnerResponse goOffline(@RequestHeader(HeaderNames.USER_ID) String userId,
                                      @RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return PartnerResponse.from(deliveryService.goOffline(UUID.fromString(userId)));
    }

    @PostMapping("/location")
    public void pingLocation(@RequestHeader(HeaderNames.USER_ID) String userId,
                              @RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions,
                              @Valid @RequestBody LocationRequest request) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        deliveryService.pingLocation(UUID.fromString(userId), request.latitude(), request.longitude());
    }
}
