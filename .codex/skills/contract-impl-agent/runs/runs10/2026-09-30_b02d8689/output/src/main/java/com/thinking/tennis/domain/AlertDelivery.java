package com.thinking.tennis.domain;

import java.time.Instant;

public final class AlertDelivery {

    private final DeliveryStatus status;
    private final Integer attemptCount;
    private final Instant lastAttemptAt;
    private final String failureReason;

    public AlertDelivery(DeliveryStatus status,
                          Integer attemptCount,
                          Instant lastAttemptAt,
                          String failureReason) {
        this.status = status;
        this.attemptCount = attemptCount;
        this.lastAttemptAt = lastAttemptAt;
        this.failureReason = failureReason;
    }

    public AlertDelivery(DeliveryStatus status,
                         long attemptCount,
                         Instant lastAttemptAt,
                         String failureReason) {
        this(status, Math.toIntExact(attemptCount), lastAttemptAt, failureReason);
    }

    public DeliveryStatus status() {
        return status;
    }

    public Integer attemptCount() {
        return attemptCount;
    }

    public Instant lastAttemptAt() {
        return lastAttemptAt;
    }

    public String failureReason() {
        return failureReason;
    }

    public AlertDelivery copy() {
        return new AlertDelivery(status, attemptCount, lastAttemptAt, failureReason);
    }
}
