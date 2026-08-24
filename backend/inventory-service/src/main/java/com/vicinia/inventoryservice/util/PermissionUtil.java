package com.vicinia.inventoryservice.util;

import com.vicinia.inventoryservice.exception.ForbiddenException;

public final class PermissionUtil {
    private PermissionUtil() {
    }

    public static void require(String permissionsHeader, String required) {
        if (!com.vicinia.common.security.PermissionUtil.hasPermission(permissionsHeader, required)) {
            throw new ForbiddenException("Requires the " + required + " permission");
        }
    }
}
