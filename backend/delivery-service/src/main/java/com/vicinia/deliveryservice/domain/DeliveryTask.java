package com.vicinia.deliveryservice.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * One row per order needing delivery, created from merchant-service's
 * order.ready event. merchantLatitude/Longitude are a snapshot taken at
 * creation time (the event payload's own data — never a live callback to
 * merchant-service) so reassignment attempts don't need to ask anyone else
 * where to search around.
 */
@Entity
@Table(name = "delivery_tasks", uniqueConstraints = @UniqueConstraint(columnNames = "order_id"))
public class DeliveryTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID orderId;
    private UUID merchantId;
    private UUID partnerId;

    private Double merchantLatitude;
    private Double merchantLongitude;

    @Enumerated(EnumType.STRING)
    private DeliveryTaskStatus status = DeliveryTaskStatus.PENDING_ASSIGNMENT;

    /**
     * Partners who've already rejected this task — excluded from the next
     * assignment search. Eager: this set is small and bounded (rarely more
     * than a couple of entries), and DeliveryService deliberately has no
     * @Transactional anywhere, so a lazy collection would throw
     * LazyInitializationException the moment a mutator like
     * rejectAndReturnToQueue touches it after the repository call returns.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "delivery_task_excluded_partners", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "partner_id")
    private Set<UUID> excludedPartnerIds = new HashSet<>();

    private Instant assignedAt;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    protected DeliveryTask() {
    }

    public DeliveryTask(UUID orderId, UUID merchantId, Double merchantLatitude, Double merchantLongitude) {
        this.orderId = orderId;
        this.merchantId = merchantId;
        this.merchantLatitude = merchantLatitude;
        this.merchantLongitude = merchantLongitude;
    }

    private void transitionTo(DeliveryTaskStatus target) {
        DeliveryTaskStatusTransition.assertAllowed(this.status, target);
        this.status = target;
        this.updatedAt = Instant.now();
    }

    public void assign(UUID partnerId) {
        transitionTo(DeliveryTaskStatus.ASSIGNED);
        this.partnerId = partnerId;
        this.assignedAt = Instant.now();
    }

    public void accept() {
        transitionTo(DeliveryTaskStatus.ACCEPTED);
    }

    /** Cycles back to PENDING_ASSIGNMENT with this partner excluded — see DeliveryTaskStatus's own class comment for why there's no separate REJECTED status. */
    public void rejectAndReturnToQueue() {
        excludedPartnerIds.add(this.partnerId);
        this.partnerId = null;
        transitionTo(DeliveryTaskStatus.PENDING_ASSIGNMENT);
    }

    public void markPickedUp() {
        transitionTo(DeliveryTaskStatus.PICKED_UP);
    }

    public void markDelivered() {
        transitionTo(DeliveryTaskStatus.DELIVERED);
    }

    // --- getters ---

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public UUID getPartnerId() {
        return partnerId;
    }

    public Double getMerchantLatitude() {
        return merchantLatitude;
    }

    public Double getMerchantLongitude() {
        return merchantLongitude;
    }

    public DeliveryTaskStatus getStatus() {
        return status;
    }

    public Set<UUID> getExcludedPartnerIds() {
        return excludedPartnerIds;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
