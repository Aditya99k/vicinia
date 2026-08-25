package com.vicinia.deliveryservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.deliveryservice.dto.DeliveryTaskResponse;
import com.vicinia.deliveryservice.service.DeliveryService;
import com.vicinia.deliveryservice.util.PermissionUtil;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** A delivery partner acting on a task assigned to them — ownership is enforced in DeliveryService.getOwnedTask, not here. */
@RestController
@RequestMapping("/api/delivery/tasks")
public class DeliveryTaskController {

    private static final String REQUIRED_PERMISSION = "DELIVERY_MANAGE";

    private final DeliveryService deliveryService;

    public DeliveryTaskController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping("/{orderId}/accept")
    public DeliveryTaskResponse accept(@RequestHeader(HeaderNames.USER_ID) String userId,
                                        @RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions,
                                        @PathVariable UUID orderId) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return DeliveryTaskResponse.from(deliveryService.accept(UUID.fromString(userId), orderId));
    }

    @PostMapping("/{orderId}/reject")
    public DeliveryTaskResponse reject(@RequestHeader(HeaderNames.USER_ID) String userId,
                                        @RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions,
                                        @PathVariable UUID orderId) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return DeliveryTaskResponse.from(deliveryService.reject(UUID.fromString(userId), orderId));
    }

    @PostMapping("/{orderId}/picked-up")
    public DeliveryTaskResponse pickedUp(@RequestHeader(HeaderNames.USER_ID) String userId,
                                          @RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions,
                                          @PathVariable UUID orderId) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return DeliveryTaskResponse.from(deliveryService.pickedUp(UUID.fromString(userId), orderId));
    }

    @PostMapping("/{orderId}/delivered")
    public DeliveryTaskResponse delivered(@RequestHeader(HeaderNames.USER_ID) String userId,
                                           @RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions,
                                           @PathVariable UUID orderId) {
        PermissionUtil.require(permissions, REQUIRED_PERMISSION);
        return DeliveryTaskResponse.from(deliveryService.delivered(UUID.fromString(userId), orderId));
    }
}
