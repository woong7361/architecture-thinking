package com.thinking.payment.domain;

import java.util.Objects;
import java.util.UUID;

public class Refund {
    private final String id;
    private final long amount;
    private final RefundType type;
    private RefundStatus status;

    public Refund(long amount, RefundType type) {
        this(UUID.randomUUID().toString(), amount, type, RefundStatus.REQUESTED);
    }

    public Refund(long amount, long cancellableAmount) {
        this(UUID.randomUUID().toString(), amount, determineType(amount, cancellableAmount), RefundStatus.REQUESTED);
    }

    public Refund(String id, long amount, RefundType type) {
        this(id, amount, type, RefundStatus.REQUESTED);
    }

    public Refund(String id, long amount, RefundType type, RefundStatus status) {
        if (amount < 0) {
            throw new IllegalArgumentException("refund amount must not be negative");
        }
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.amount = amount;
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public static Refund requested(long amount, long cancellableAmount) {
        return new Refund(amount, cancellableAmount);
    }

    public static Refund requested(long amount, RefundType type) {
        return new Refund(amount, type);
    }

    public void succeed() {
        transitionToTerminal(RefundStatus.SUCCEEDED);
    }

    public void markSucceeded() {
        succeed();
    }

    public void fail() {
        transitionToTerminal(RefundStatus.FAILED);
    }

    public void markFailed() {
        fail();
    }

    public void timeOut() {
        transitionToTerminal(RefundStatus.TIMED_OUT);
    }

    public void timeout() {
        timeOut();
    }

    public void markTimedOut() {
        timeOut();
    }

    private void transitionToTerminal(RefundStatus nextStatus) {
        if (status != RefundStatus.REQUESTED) {
            throw new RefundException("refund is already completed: " + status);
        }
        status = nextStatus;
    }

    private static RefundType determineType(long amount, long cancellableAmount) {
        if (amount < 0) {
            throw new IllegalArgumentException("refund amount must not be negative");
        }
        if (cancellableAmount <= 0) {
            throw new IllegalArgumentException("cancellable amount must be positive");
        }
        if (amount > cancellableAmount) {
            throw new RefundException("refund amount exceeds cancellable amount");
        }
        return amount == cancellableAmount ? RefundType.FULL : RefundType.PARTIAL;
    }

    public String getId() {
        return id;
    }

    public long getAmount() {
        return amount;
    }

    public RefundType getType() {
        return type;
    }

    public RefundStatus getStatus() {
        return status;
    }
}
