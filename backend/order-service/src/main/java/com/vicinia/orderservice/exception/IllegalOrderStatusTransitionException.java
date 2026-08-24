package com.vicinia.orderservice.exception;

import com.vicinia.orderservice.domain.OrderStatus;

public class IllegalOrderStatusTransitionException extends RuntimeException {
    public IllegalOrderStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Cannot transition order from " + from + " to " + to);
    }
}
