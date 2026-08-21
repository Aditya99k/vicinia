package com.vicinia.authservice.messaging;

import java.util.Set;

/** Consumed by user-service to create the matching UserProfile row. */
public record UserRegisteredPayload(String userId, String email, Set<String> roles) {
}
