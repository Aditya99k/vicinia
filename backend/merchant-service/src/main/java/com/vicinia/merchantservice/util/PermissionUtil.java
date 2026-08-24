package com.vicinia.merchantservice.util;

import com.vicinia.merchantservice.exception.ForbiddenException;

/** Thin wrapper: the actual header-parsing logic lives in common (shared with catalog-service and beyond); the exception thrown stays local to this service. */
public final class PermissionUtil {

    private PermissionUtil() {
    }

    public static void require(String permissionsHeader, String required) {
        if (!com.vicinia.common.security.PermissionUtil.hasPermission(permissionsHeader, required)) {
            throw new ForbiddenException("Requires the " + required + " permission");
        }
    }
}
