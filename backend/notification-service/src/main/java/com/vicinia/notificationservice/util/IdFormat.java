package com.vicinia.notificationservice.util;

/** Notification bodies read better with the same short "#XXXXXXXX" order reference the frontend already displays everywhere, not a raw 36-character UUID. */
public final class IdFormat {
    private IdFormat() {
    }

    public static String shorten(String id) {
        return id != null && id.length() > 8 ? id.substring(0, 8) : id;
    }
}
