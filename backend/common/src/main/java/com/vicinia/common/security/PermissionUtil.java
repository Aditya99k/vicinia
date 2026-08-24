package com.vicinia.common.security;

import java.util.Arrays;

/**
 * Parses api-gateway's comma-separated X-User-Permissions header. Returns a
 * boolean rather than throwing — each service keeps its own exception type
 * (matching the existing per-service exception packages) and throws it
 * itself; only the actual parsing logic is shared here, needed by every
 * service with admin-gated endpoints (merchant-service in Stage 3,
 * catalog-service in Stage 4, more to come).
 */
public final class PermissionUtil {

    private PermissionUtil() {
    }

    public static boolean hasPermission(String permissionsHeader, String required) {
        return permissionsHeader != null
                && Arrays.asList(permissionsHeader.split(",")).contains(required);
    }
}
