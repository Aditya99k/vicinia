package com.vicinia.merchantservice.util;

/**
 * Great-circle (Haversine) distance in km — the same formula the frontend
 * previously had no real equivalent for (see deliveryEstimate.js's own
 * comment on why it faked a distance from a hash: customer addresses had
 * no coordinates at all until Stage 18's geolocation work, and merchant
 * coordinates were seed/test values with no relation to any real
 * customer location). Both sides now carry real lat/lng, so this replaces
 * that fake estimate with an actual distance wherever a customer's own
 * coordinates are available.
 */
public final class GeoDistance {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoDistance() {
    }

    public static double km(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
