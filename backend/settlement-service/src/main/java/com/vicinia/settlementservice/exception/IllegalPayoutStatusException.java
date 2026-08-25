package com.vicinia.settlementservice.exception;

import com.vicinia.settlementservice.domain.PayoutStatus;

public class IllegalPayoutStatusException extends RuntimeException {
    public IllegalPayoutStatusException(PayoutStatus from, PayoutStatus to) {
        super("Cannot transition payout from " + from + " to " + to);
    }
}
