package com.vicinia.orderservice.util;

import com.vicinia.orderservice.exception.ForbiddenException;

public final class PermissionUtil {
    private PermissionUtil() {
    }

    public static void require(String permissionsHeader, String required) {
        if (!com.vicinia.common.security.PermissionUtil.hasPermission(permissionsHeader, required)) {
            throw new ForbiddenException("Requires the " + required + " permission");
        }
    }
}
