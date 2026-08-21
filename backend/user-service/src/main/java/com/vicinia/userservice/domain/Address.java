package com.vicinia.userservice.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;

    private String label;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String pincode;
    private boolean isDefault;

    protected Address() {
    }

    public Address(UUID userId, String label, String line1, String line2,
                    String city, String state, String pincode, boolean isDefault) {
        this.userId = userId;
        this.label = label;
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.isDefault = isDefault;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getLabel() {
        return label;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
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

    public boolean isDefault() {
        return isDefault;
    }

    public void update(String label, String line1, String line2,
                        String city, String state, String pincode, boolean isDefault) {
        this.label = label;
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.isDefault = isDefault;
    }
}
