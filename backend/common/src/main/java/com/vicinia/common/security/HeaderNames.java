package com.vicinia.common.security;

/**
 * Header contract between api-gateway and every domain service
 * (ARCHITECTURE.md §14). Services trust these headers only because the
 * gateway is the only caller allowed to set them — see InternalRequestFilter.
 */
public final class HeaderNames {

    private HeaderNames() {
    }

    public static final String INTERNAL_SECRET = "X-Internal-Secret";
    public static final String USER_ID = "X-User-Id";
    public static final String USER_EMAIL = "X-User-Email";
    public static final String USER_ROLES = "X-User-Roles";
    public static final String USER_PERMISSIONS = "X-User-Permissions";
    /** Generated at api-gateway if absent (Stage 16, ARCHITECTURE.md §15), carried through every internal call and into Kafka record headers so one request's trail is traceable across every service and topic it touches. */
    public static final String CORRELATION_ID = "X-Correlation-Id";
}
