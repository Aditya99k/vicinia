package com.vicinia.notificationservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.notificationservice.dto.NotificationResponse;
import com.vicinia.notificationservice.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Customer-facing, gateway-routed — scoped to the caller's own notifications (X-User-Id), same pattern as order-service's /mine. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/mine")
    public List<NotificationResponse> mine(@RequestHeader(HeaderNames.USER_ID) String userId) {
        return notificationService.mine(userId).stream().map(NotificationResponse::from).toList();
    }
}
