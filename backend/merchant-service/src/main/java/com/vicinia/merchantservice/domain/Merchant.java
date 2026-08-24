package com.vicinia.merchantservice.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One row per store. ownerUserId correlates to auth-service's
 * UserCredential.id by value, not a foreign key — different database,
 * same pattern as user-service's UserProfile (ARCHITECTURE.md §6).
 */
@Entity
@Table(name = "merchants", uniqueConstraints = @UniqueConstraint(columnNames = "owner_user_id"))
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID ownerUserId;

    private String storeName;
    private String description;

    private String addressLine1;
    private String city;
    private String state;
    private String pincode;

    private Double latitude;
    private Double longitude;
    private Double deliveryRadiusKm = 5.0;

    private LocalTime openTime;
    private LocalTime closeTime;

    @Enumerated(EnumType.STRING)
    private MerchantStatus status = MerchantStatus.PENDING_REVIEW;

    private String rejectionReason;
    private String suspensionReason;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("uploadedAt")
    private List<MerchantDocument> documents = new ArrayList<>();

    protected Merchant() {
    }

    public Merchant(UUID ownerUserId, String storeName, String description,
                     String addressLine1, String city, String state, String pincode) {
        this.ownerUserId = ownerUserId;
        this.storeName = storeName;
        this.description = description;
        this.addressLine1 = addressLine1;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
    }

    public void transitionTo(MerchantStatus target) {
        MerchantStatusTransition.assertAllowed(this.status, target);
        this.status = target;
        this.updatedAt = Instant.now();
    }

    public void addDocument(MerchantDocument document) {
        document.setMerchant(this);
        documents.add(document);
    }

    public void updateProfile(String storeName, String description, String addressLine1,
                               String city, String state, String pincode,
                               Double latitude, Double longitude, Double deliveryRadiusKm) {
        this.storeName = storeName;
        this.description = description;
        this.addressLine1 = addressLine1;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.latitude = latitude;
        this.longitude = longitude;
        if (deliveryRadiusKm != null) {
            this.deliveryRadiusKm = deliveryRadiusKm;
        }
        this.updatedAt = Instant.now();
    }

    public void updateHours(LocalTime openTime, LocalTime closeTime) {
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.updatedAt = Instant.now();
    }

    public boolean hasHoursSet() {
        return openTime != null && closeTime != null;
    }

    // --- getters ---

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getDescription() {
        return description;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPincode() {
        return pincode;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getDeliveryRadiusKm() {
        return deliveryRadiusKm;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public MerchantStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getSuspensionReason() {
        return suspensionReason;
    }

    public void setSuspensionReason(String suspensionReason) {
        this.suspensionReason = suspensionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<MerchantDocument> getDocuments() {
        return documents;
    }
}
