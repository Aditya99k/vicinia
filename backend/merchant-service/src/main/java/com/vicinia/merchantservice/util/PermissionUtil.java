package com.vicinia.merchantservice.util;

import com.vicinia.merchantservice.exception.ForbiddenException;

import java.util.Arrays;

/**
 * api-gateway injects X-User-Permissions as a comma-separated list from the
 * verified JWT (ARCHITECTURE.md §14) — this is a plain header check, not a
 * Spring Security authorization framework, matching the hand-rolled style
 * already used for JWT/internal-secret handling elsewhere in the system.
 */
public final class PermissionUtil {

    private PermissionUtil() {
    }

    public static void require(String permissionsHeader, String required) {
        boolean has = permissionsHeader != null
                && Arrays.asList(permissionsHeader.split(",")).contains(required);
        if (!has) {
            throw new ForbiddenException("Requires the " + required + " permission");
        }
    }
}
