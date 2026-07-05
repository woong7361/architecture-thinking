package com.thinking.payment;

public final class Order {

    private final String paymentUuid;
    private final int amount;
    private final int totalDays;
    private final PaymentPlatform paymentPlatform;
    private OrderStatus status;
    private int refundedAmount;

    private Order(String paymentUuid, int amount, int totalDays, PaymentPlatform paymentPlatform, OrderStatus status, int refundedAmount) {
        this.paymentUuid = paymentUuid;
        this.amount = amount;
        this.totalDays = totalDays;
        this.paymentPlatform = paymentPlatform;
        this.status = status;
        this.refundedAmount = refundedAmount;
    }

    public static Order paid(String paymentUuid, int amount) {
        return paid(paymentUuid, amount, 30);
    }

    public static Order paid(String paymentUuid, int amount, int totalDays) {
        return new Order(paymentUuid, amount, totalDays, PaymentPlatform.WEB, OrderStatus.PAID, 0);
    }

    public static Order pending(String paymentUuid, int amount, int totalDays) {
        return new Order(paymentUuid, amount, totalDays, PaymentPlatform.WEB, OrderStatus.PENDING, 0);
    }

    public static Order paidOn(String paymentUuid, int amount, int totalDays, PaymentPlatform paymentPlatform) {
        return new Order(paymentUuid, amount, totalDays, paymentPlatform, OrderStatus.PAID, 0);
    }

    public static Order partiallyRefunded(String paymentUuid, int amount, int totalDays, int refundedAmount) {
        return new Order(paymentUuid, amount, totalDays, PaymentPlatform.WEB, OrderStatus.PARTIALLY_REFUNDED, refundedAmount);
    }

    public static Order refunded(String paymentUuid, int amount, int totalDays) {
        return new Order(paymentUuid, amount, totalDays, PaymentPlatform.WEB, OrderStatus.REFUNDED, amount);
    }

    String paymentUuid() {
        return paymentUuid;
    }

    int amount() {
        return amount;
    }

    int totalDays() {
        return totalDays;
    }

    PaymentPlatform paymentPlatform() {
        return paymentPlatform;
    }

    public OrderStatus status() {
        return status;
    }

    public int refundableAmount() {
        return amount - refundedAmount;
    }

    void applyRefund(int refundAmount) {
        if (refundAmount <= 0) {
            throw new RefundRejectedException(
                RefundRejectionReason.INVALID_REFUND_AMOUNT,
                "환불 금액은 0원보다 커야 합니다: " + refundAmount);
        }
        if (refundAmount > refundableAmount()) {
            throw new RefundRejectedException(
                RefundRejectionReason.REFUND_AMOUNT_EXCEEDED,
                "환불 금액이 환불 가능 금액을 초과했습니다: amount=" + refundAmount + ", refundable=" + refundableAmount());
        }

        refundedAmount += refundAmount;
        status = refundedAmount == amount ? OrderStatus.REFUNDED : OrderStatus.PARTIALLY_REFUNDED;
    }
}
