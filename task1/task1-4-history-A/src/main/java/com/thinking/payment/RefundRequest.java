package com.thinking.payment;

public record RefundRequest(RefundPolicy policy, Integer manualAmount, int elapsedDays) {

    public static RefundRequest proration(int elapsedDays) {
        return new RefundRequest(RefundPolicy.PRORATION, null, elapsedDays);
    }

    public static RefundRequest manual(int amount) {
        return new RefundRequest(RefundPolicy.MANUAL, amount, 0);
    }

    public static RefundRequest manualWithoutAmount() {
        return new RefundRequest(RefundPolicy.MANUAL, null, 0);
    }
}
