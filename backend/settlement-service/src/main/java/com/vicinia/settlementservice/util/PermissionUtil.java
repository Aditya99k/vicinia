package com.vicinia.settlementservice.util;

import com.vicinia.settlementservice.exception.ForbiddenException;

/** Thin wrapper: the actual header-parsing logic lives in common; the exception thrown stays local to this service. */
public final class PermissionUtil {

    private PermissionUtil() {
    }

    public static void require(String permissionsHeader, String required) {
        if (!com.vicinia.common.security.PermissionUtil.hasPermission(permissionsHeader, required)) {
            throw new ForbiddenException("Requires the " + required + " permission");
        }
    }
}
