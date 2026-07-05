package com.thinking.payment.domain;

import java.util.Objects;
import java.util.UUID;

public class Order {
    private final String id;
    private final long amount;
    private long canceledAmount;
    private OrderStatus status;

    public Order(long amount) {
        this(UUID.randomUUID().toString(), amount, 0, OrderStatus.PAID);
    }

    public Order(long amount, OrderStatus status) {
        this(UUID.randomUUID().toString(), amount, 0, status);
    }

    public Order(long amount, long canceledAmount, OrderStatus status) {
        this(UUID.randomUUID().toString(), amount, canceledAmount, status);
    }

    public Order(String id, long amount, OrderStatus status) {
        this(id, amount, 0, status);
    }

    public Order(String id, long amount, long canceledAmount, OrderStatus status) {
        if (amount <= 0) {
            throw new IllegalArgumentException("order amount must be positive");
        }
        if (canceledAmount < 0 || canceledAmount > amount) {
            throw new IllegalArgumentException("canceled amount must be between 0 and order amount");
        }
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.amount = amount;
        this.canceledAmount = canceledAmount;
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public void validateRefundable() {
        if (!isRefundable()) {
            throw new RefundException("order is not refundable: " + status);
        }
        if (getCancellableAmount() <= 0) {
            throw new RefundException("cancellable amount does not remain");
        }
    }

    public boolean isRefundable() {
        return status == OrderStatus.PAID || status == OrderStatus.PARTIALLY_REFUNDED;
    }

    public long cancellableAmount() {
        return getCancellableAmount();
    }

    public long getCancellableAmount() {
        return amount - canceledAmount;
    }

    public RefundType determineRefundType(long refundAmount) {
        validateRefundAmount(refundAmount);
        return refundAmount == getCancellableAmount() ? RefundType.FULL : RefundType.PARTIAL;
    }

    public void applyRefund(long refundAmount) {
        validateRefundable();
        validateRefundAmount(refundAmount);

        if (refundAmount == 0) {
            return;
        }

        canceledAmount += refundAmount;
        status = canceledAmount == amount ? OrderStatus.REFUNDED : OrderStatus.PARTIALLY_REFUNDED;
    }

    public void refund(long refundAmount) {
        applyRefund(refundAmount);
    }

    private void validateRefundAmount(long refundAmount) {
        if (refundAmount < 0) {
            throw new RefundException("refund amount must not be negative");
        }
        if (refundAmount > getCancellableAmount()) {
            throw new RefundException("refund amount exceeds cancellable amount");
        }
    }

    public String getId() {
        return id;
    }

    public long getAmount() {
        return amount;
    }

    public long getCanceledAmount() {
        return canceledAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }
}
