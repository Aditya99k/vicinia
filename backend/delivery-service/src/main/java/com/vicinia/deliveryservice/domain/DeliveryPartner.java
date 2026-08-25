package com.vicinia.deliveryservice.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * One row per delivery partner, auto-provisioned on user.registered when
 * the user's roles include DELIVERY_PARTNER — mirroring payment-service's
 * wallet auto-provisioning (Stage 8) and user-service's own profile
 * auto-creation (Stage 2). No lat/lng column here — live location lives
 * only in Redis GEO (PartnerGeoService), never persisted per-ping.
 */
@Entity
@Table(name = "delivery_partners", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class DeliveryPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;

    @Enumerated(EnumType.STRING)
    private PartnerStatus status = PartnerStatus.OFFLINE;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    protected DeliveryPartner() {
    }

    public DeliveryPartner(UUID userId) {
        this.userId = userId;
    }

    public void goOnline() {
        this.status = PartnerStatus.ONLINE;
        this.updatedAt = Instant.now();
    }

    public void goOffline() {
        this.status = PartnerStatus.OFFLINE;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public PartnerStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
