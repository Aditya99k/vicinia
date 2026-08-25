package com.vicinia.deliveryservice.exception;

import com.vicinia.deliveryservice.domain.DeliveryTaskStatus;

public class IllegalDeliveryTaskStatusException extends RuntimeException {
    public IllegalDeliveryTaskStatusException(DeliveryTaskStatus from, DeliveryTaskStatus to) {
        super("Cannot transition delivery task from " + from + " to " + to);
    }
}
