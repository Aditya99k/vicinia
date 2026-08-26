package com.vicinia.deliveryservice.service;

import com.vicinia.deliveryservice.domain.DeliveryPartner;
import com.vicinia.deliveryservice.domain.DeliveryTask;
import com.vicinia.deliveryservice.domain.DeliveryTaskStatus;
import com.vicinia.deliveryservice.exception.DeliveryPartnerNotFoundException;
import com.vicinia.deliveryservice.exception.DeliveryTaskNotFoundException;
import com.vicinia.deliveryservice.exception.ForbiddenException;
import com.vicinia.deliveryservice.messaging.DeliveryEventPublisher;
import com.vicinia.deliveryservice.repository.DeliveryPartnerRepository;
import com.vicinia.deliveryservice.repository.DeliveryTaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * No @Transactional anywhere — same reasoning as OrderService (Stage 8):
 * every write here is exactly one repository.save() call (already
 * atomically transactional via Spring Data JPA on its own), and
 * attemptAssignment's Redis GEO search + Kafka publish are external calls
 * that must never sit inside a still-open DB transaction that could roll
 * back after they've already had a real effect.
 */
@Service
public class DeliveryService {

    private final DeliveryPartnerRepository partnerRepository;
    private final DeliveryTaskRepository taskRepository;
    private final PartnerGeoService geoService;
    private final DeliveryEventPublisher eventPublisher;

    public DeliveryService(DeliveryPartnerRepository partnerRepository, DeliveryTaskRepository taskRepository,
                            PartnerGeoService geoService, DeliveryEventPublisher eventPublisher) {
        this.partnerRepository = partnerRepository;
        this.taskRepository = taskRepository;
        this.geoService = geoService;
        this.eventPublisher = eventPublisher;
    }

    // --- partner lifecycle ---

    public void provisionIfAbsent(UUID userId) {
        if (partnerRepository.existsByUserId(userId)) {
            return;
        }
        partnerRepository.save(new DeliveryPartner(userId));
    }

    public DeliveryPartner goOnline(UUID userId, double latitude, double longitude) {
        DeliveryPartner partner = getByUserId(userId);
        partner.goOnline();
        DeliveryPartner saved = partnerRepository.save(partner);
        geoService.addOrUpdate(partner.getId(), latitude, longitude);
        return saved;
    }

    public DeliveryPartner goOffline(UUID userId) {
        DeliveryPartner partner = getByUserId(userId);
        partner.goOffline();
        DeliveryPartner saved = partnerRepository.save(partner);
        geoService.remove(partner.getId());
        return saved;
    }

    public void pingLocation(UUID userId, double latitude, double longitude) {
        DeliveryPartner partner = getByUserId(userId);
        geoService.addOrUpdate(partner.getId(), latitude, longitude);
    }

    public DeliveryPartner me(UUID userId) {
        return getByUserId(userId);
    }

    // --- task lifecycle ---

    private static final List<DeliveryTaskStatus> ACTIVE_TASK_STATUSES =
            List.of(DeliveryTaskStatus.ASSIGNED, DeliveryTaskStatus.ACCEPTED, DeliveryTaskStatus.PICKED_UP);

    /**
     * The rider's own currently-active task(s), if any — lets the delivery
     * app poll for a fresh assignment and auto-load it instead of relying
     * only on the "New pickup assigned" notification's deep link or the
     * rider typing an order ID in by hand.
     */
    public List<DeliveryTask> myActiveTasks(UUID userId) {
        DeliveryPartner partner = getByUserId(userId);
        return taskRepository.findByPartnerIdAndStatusInOrderByAssignedAtDesc(partner.getId(), ACTIVE_TASK_STATUSES);
    }

    /** Idempotent on orderId — a replayed order.ready is a safe no-op, matching every other Kafka-driven idempotency in this project. */
    public void createAndAssign(UUID orderId, UUID merchantId, Double latitude, Double longitude) {
        if (taskRepository.findByOrderId(orderId).isPresent()) {
            return;
        }
        DeliveryTask task = taskRepository.save(new DeliveryTask(orderId, merchantId, latitude, longitude));
        attemptAssignment(task);
    }

    /** Called on creation, on a partner's reject (immediate retry), and by AssignmentReaper on a schedule for tasks that found nobody available the first time. A no-op if nobody's online within range — the task just stays PENDING_ASSIGNMENT for the reaper to retry. */
    public void attemptAssignment(DeliveryTask task) {
        List<UUID> nearest = geoService.findNearest(
                task.getMerchantLatitude(), task.getMerchantLongitude(), task.getExcludedPartnerIds(), 1);
        if (nearest.isEmpty()) {
            return;
        }
        UUID partnerId = nearest.get(0);
        task.assign(partnerId);
        taskRepository.save(task);
        eventPublisher.publishAssigned(task.getOrderId(), partnerId);
    }

    public DeliveryTask accept(UUID userId, UUID orderId) {
        DeliveryPartner partner = getByUserId(userId);
        DeliveryTask task = getOwnedTask(partner.getId(), orderId);
        task.accept();
        return taskRepository.save(task);
    }

    public DeliveryTask reject(UUID userId, UUID orderId) {
        DeliveryPartner partner = getByUserId(userId);
        DeliveryTask task = getOwnedTask(partner.getId(), orderId);
        task.rejectAndReturnToQueue();
        DeliveryTask saved = taskRepository.save(task);
        attemptAssignment(saved);
        return saved;
    }

    public DeliveryTask pickedUp(UUID userId, UUID orderId) {
        DeliveryPartner partner = getByUserId(userId);
        DeliveryTask task = getOwnedTask(partner.getId(), orderId);
        task.markPickedUp();
        return taskRepository.save(task);
    }

    public DeliveryTask delivered(UUID userId, UUID orderId) {
        DeliveryPartner partner = getByUserId(userId);
        DeliveryTask task = getOwnedTask(partner.getId(), orderId);
        task.markDelivered();
        DeliveryTask saved = taskRepository.save(task);
        eventPublisher.publishDelivered(saved.getOrderId());
        return saved;
    }

    private DeliveryTask getOwnedTask(UUID partnerId, UUID orderId) {
        DeliveryTask task = taskRepository.findByOrderId(orderId).orElseThrow(() -> new DeliveryTaskNotFoundException(orderId));
        if (!partnerId.equals(task.getPartnerId())) {
            throw new ForbiddenException("This delivery task is not assigned to you");
        }
        return task;
    }

    private DeliveryPartner getByUserId(UUID userId) {
        return partnerRepository.findByUserId(userId).orElseThrow(() -> new DeliveryPartnerNotFoundException(userId));
    }
}
