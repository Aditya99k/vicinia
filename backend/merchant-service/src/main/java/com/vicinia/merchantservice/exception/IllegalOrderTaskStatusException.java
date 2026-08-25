package com.vicinia.merchantservice.exception;

import com.vicinia.merchantservice.domain.OrderTaskStatus;

public class IllegalOrderTaskStatusException extends RuntimeException {
    public IllegalOrderTaskStatusException(OrderTaskStatus from, OrderTaskStatus to) {
        super("Cannot transition order task from " + from + " to " + to);
    }
}
