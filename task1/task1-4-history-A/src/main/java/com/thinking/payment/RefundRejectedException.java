package com.thinking.payment;

public final class RefundRejectedException extends IllegalArgumentException {

    private final RefundRejectionReason reason;

    public RefundRejectedException(RefundRejectionReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public RefundRejectionReason reason() {
        return reason;
    }
}
