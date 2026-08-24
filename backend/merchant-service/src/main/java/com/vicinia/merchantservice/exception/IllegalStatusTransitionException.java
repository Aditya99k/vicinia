package com.vicinia.merchantservice.exception;

import com.vicinia.merchantservice.domain.MerchantStatus;

public class IllegalStatusTransitionException extends RuntimeException {
    public IllegalStatusTransitionException(MerchantStatus from, MerchantStatus to) {
        super("Cannot move a merchant from " + from + " to " + to);
    }
}
