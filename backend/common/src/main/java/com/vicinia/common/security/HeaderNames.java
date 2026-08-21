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
}
