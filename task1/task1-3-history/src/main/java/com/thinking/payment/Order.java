package com.thinking.payment;

public final class Order {

    private final String paymentUuid;
    private final int amount;
    private OrderStatus status;

    private Order(String paymentUuid, int amount) {
        this.paymentUuid = paymentUuid;
        this.amount = amount;
        this.status = OrderStatus.PAID;
    }

    public static Order paid(String paymentUuid, int amount) {
        return new Order(paymentUuid, amount);
    }

    String paymentUuid() {
        return paymentUuid;
    }

    int amount() {
        return amount;
    }

    public OrderStatus status() {
        return status;
    }

    void applyRefund(int refundAmount) {
        status = OrderStatus.REFUNDED;
    }
}
