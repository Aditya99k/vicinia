package com.vicinia.deliveryservice.service;

import com.vicinia.deliveryservice.domain.DeliveryTask;
import com.vicinia.deliveryservice.domain.DeliveryTaskStatus;
import com.vicinia.deliveryservice.repository.DeliveryTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** Same scheduled-reaper pattern as Stage 5's ReservationReaper — a task that found nobody online the first time just stays PENDING_ASSIGNMENT, retried here on a fixed interval rather than lost. */
@Component
public class AssignmentReaper {

    private static final Logger log = LoggerFactory.getLogger(AssignmentReaper.class);

    private final DeliveryTaskRepository taskRepository;
    private final DeliveryService deliveryService;

    public AssignmentReaper(DeliveryTaskRepository taskRepository, DeliveryService deliveryService) {
        this.taskRepository = taskRepository;
        this.deliveryService = deliveryService;
    }

    @Scheduled(fixedDelayString = "${vicinia.delivery.reassignment-interval-ms:30000}")
    public void retryPendingAssignments() {
        List<DeliveryTask> pending = taskRepository.findByStatus(DeliveryTaskStatus.PENDING_ASSIGNMENT);
        if (pending.isEmpty()) {
            return;
        }
        pending.forEach(deliveryService::attemptAssignment);
        log.debug("Retried assignment for {} pending task(s)", pending.size());
    }
}
