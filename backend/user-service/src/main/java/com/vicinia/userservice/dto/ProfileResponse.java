package com.vicinia.userservice.dto;

public record ProfileResponse(String userId, String email, String fullName, String phone) {
}
