package com.vicinia.merchantservice.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Document *metadata* only — a type and a reference URL the client
 * supplies. No file upload/storage is wired up yet (that's a Cloudinary
 * integration per DEPLOYMENT.md, out of scope here); a real upload flow
 * would populate referenceUrl with the resulting hosted URL.
 */
@Entity
@Table(name = "merchant_documents")
public class MerchantDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    private String documentType;
    private String referenceUrl;
    private Instant uploadedAt = Instant.now();

    protected MerchantDocument() {
    }

    public MerchantDocument(String documentType, String referenceUrl) {
        this.documentType = documentType;
        this.referenceUrl = referenceUrl;
    }

    public UUID getId() {
        return id;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getReferenceUrl() {
        return referenceUrl;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
