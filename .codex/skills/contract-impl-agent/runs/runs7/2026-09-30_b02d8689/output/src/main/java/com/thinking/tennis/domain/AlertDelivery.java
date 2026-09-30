package com.thinking.tennis.domain;

import java.time.Instant;

public record AlertDelivery(
        DeliveryStatus status,
        int attemptCount,
        Instant lastAttemptAt,
        String failureReason
) {
    public AlertDelivery {
        if (attemptCount < 0 || attemptCount > 4) {
            throw new IllegalArgumentException("attemptCount must be between 0 and 4");
        }
    }
}
