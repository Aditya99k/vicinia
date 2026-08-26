package com.vicinia.userservice.dto;

import com.vicinia.userservice.domain.Address;

public record AddressResponse(
        String id, String label, String line1, String line2,
        String city, String state, String pincode, boolean isDefault,
        Double latitude, Double longitude
) {
    public static AddressResponse from(Address a) {
        return new AddressResponse(a.getId().toString(), a.getLabel(), a.getLine1(), a.getLine2(),
                a.getCity(), a.getState(), a.getPincode(), a.isDefault(),
                a.getLatitude(), a.getLongitude());
    }
}
