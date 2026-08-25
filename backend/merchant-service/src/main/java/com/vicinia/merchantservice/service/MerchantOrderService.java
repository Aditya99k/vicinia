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

    public List<MerchantOrderTask> pending(UUID ownerUserId) {
        return taskRepository.findByMerchantIdAndStatusOrderByCreatedAtAsc(ownerUserId, OrderTaskStatus.PENDING_ACCEPTANCE);
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

    private MerchantOrderTask getOwnedTask(UUID merchantId, UUID orderId) {
        MerchantOrderTask task = taskRepository.findByOrderId(orderId)
                .orElseThrow(() -> new MerchantOrderTaskNotFoundException(orderId));
        if (!task.getMerchantId().equals(merchantId)) {
            throw new ForbiddenException("This order does not belong to your store");
        }
        return task;
    }
}
