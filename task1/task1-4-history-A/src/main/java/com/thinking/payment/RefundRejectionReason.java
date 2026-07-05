package com.thinking.payment;

public enum RefundRejectionReason {
    INVALID_SUBSCRIPTION_PERIOD,
    INVALID_REFUND_AMOUNT,
    REFUND_AMOUNT_EXCEEDED,
    NOT_REFUNDABLE,
    WEB_ONLY
}
