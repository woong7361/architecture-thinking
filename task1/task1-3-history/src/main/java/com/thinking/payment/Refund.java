package com.thinking.payment;

public final class Refund {

    private final int totalDays;
    private final int elapsedDays;
    private RefundStatus status;

    private Refund(int totalDays, int elapsedDays) {
        this.totalDays = totalDays;
        this.elapsedDays = elapsedDays;
        this.status = RefundStatus.REQUESTED;
    }

    public static Refund proration(int totalDays, int elapsedDays) {
        return new Refund(totalDays, elapsedDays);
    }

    int amountFor(int price) {
        return RefundCalculator.calculate(price, totalDays, elapsedDays);
    }

    public RefundStatus status() {
        return status;
    }

    void succeed() {
        status = RefundStatus.SUCCEEDED;
    }
}
