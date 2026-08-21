package com.vicinia.userservice.exception;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException(String userId) {
        super("No profile found for user " + userId
                + " — it's created asynchronously from user.registered and may not have landed yet");
    }
}
