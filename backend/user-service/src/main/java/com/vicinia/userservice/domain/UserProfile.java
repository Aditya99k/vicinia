package com.vicinia.userservice.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * The id here is the same UUID auth-service issued for the credential —
 * correlated by value across the two services' separate databases, not a
 * foreign key (ARCHITECTURE.md §6: each service owns its own database).
 * Created by UserEventConsumer reacting to user.registered, never by a
 * direct API call — there is no POST /users endpoint.
 */
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    private UUID id;

    private String email;

    private String fullName;

    private String phone;

    private Instant createdAt = Instant.now();

    protected UserProfile() {
    }

    public UserProfile(UUID id, String email) {
        this.id = id;
        this.email = email;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
