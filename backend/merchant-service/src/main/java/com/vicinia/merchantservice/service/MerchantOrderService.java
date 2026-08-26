package com.vicinia.merchantservice.service;

import com.vicinia.merchantservice.domain.Merchant;
import com.vicinia.merchantservice.domain.MerchantOrderTask;
import com.vicinia.merchantservice.domain.OrderTaskStatus;
import com.vicinia.merchantservice.exception.ForbiddenException;
import com.vicinia.merchantservice.exception.MerchantOrderTaskNotFoundException;
import com.vicinia.merchantservice.exception.StoreLocationNotSetException;
import com.vicinia.merchantservice.messaging.MerchantOrderEventPublisher;
import com.vicinia.merchantservice.repository.MerchantOrderTaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MerchantOrderService {

    private final MerchantOrderTaskRepository taskRepository;
    private final MerchantService merchantService;
    private final MerchantOrderEventPublisher eventPublisher;

    public MerchantOrderService(MerchantOrderTaskRepository taskRepository, MerchantService merchantService,
                                 MerchantOrderEventPublisher eventPublisher) {
        this.taskRepository = taskRepository;
        this.merchantService = merchantService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * The merchant's action-or-attention queue: PENDING_ACCEPTANCE (needs
     * accept/reject), ACCEPTED (needs mark-ready), and READY (waiting at
     * the counter for a rider to actually show up and collect it) all
     * belong here. PENDING_ACCEPTANCE alone (the original shape here)
     * meant an order vanished from every merchant's queue the instant they
     * accepted it, with no way back to it to ever mark it ready; stopping
     * at ACCEPTED (the next fix) then meant a READY order vanished from
     * sight the moment it was marked ready, with no record left for the
     * merchant to check a rider's pickup against. It only leaves this list
     * once COMPLETED, driven by completeFromDelivery below.
     */
    public List<MerchantOrderTask> pending(UUID ownerUserId) {
        return taskRepository.findByMerchantIdAndStatusInOrderByCreatedAtDesc(
                ownerUserId, List.of(OrderTaskStatus.PENDING_ACCEPTANCE, OrderTaskStatus.ACCEPTED, OrderTaskStatus.READY));
    }

    public MerchantOrderTask accept(UUID ownerUserId, UUID orderId) {
        MerchantOrderTask task = getOwnedTask(ownerUserId, orderId);
        task.transitionTo(OrderTaskStatus.ACCEPTED);
        taskRepository.save(task);
        eventPublisher.publishAccepted(orderId);
        return task;
    }

    public MerchantOrderTask reject(UUID ownerUserId, UUID orderId, String reason) {
        MerchantOrderTask task = getOwnedTask(ownerUserId, orderId);
        task.reject(reason);
        taskRepository.save(task);
        eventPublisher.publishRejected(orderId, reason);
        return task;
    }

    /** Validates the store has a location set — delivery-service needs it to search for a nearby partner, and it comes along in the event, not a callback. */
    public MerchantOrderTask markReady(UUID ownerUserId, UUID orderId) {
        Merchant merchant = merchantService.getMine(ownerUserId);
        if (merchant.getLatitude() == null || merchant.getLongitude() == null) {
            throw new StoreLocationNotSetException();
        }

        MerchantOrderTask task = getOwnedTask(ownerUserId, orderId);
        task.transitionTo(OrderTaskStatus.READY);
        taskRepository.save(task);
        eventPublisher.publishReady(orderId, ownerUserId, merchant.getLatitude(), merchant.getLongitude());
        return task;
    }

    /** From delivery-service's delivery.delivered event — the only thing that finally clears a READY task out of the merchant's queue. Idempotent (only transitions while still READY), same as every other Kafka-driven handler in this project. */
    public void completeFromDelivery(UUID orderId) {
        taskRepository.findByOrderId(orderId).ifPresent(task -> {
            if (task.getStatus() != OrderTaskStatus.READY) {
                return;
            }
            task.transitionTo(OrderTaskStatus.COMPLETED);
            taskRepository.save(task);
        });
    }

    private MerchantOrderTask getOwnedTask(UUID merchantId, UUID orderId) {
        MerchantOrderTask task = taskRepository.findByOrderId(orderId)
                .orElseThrow(() -> new MerchantOrderTaskNotFoundException(orderId));
        if (!task.getMerchantId().equals(merchantId)) {
            throw new ForbiddenException("This order does not belong to your store");
        }
        return task;
    }
}
