package com.thinking.tennis.domain;

import java.time.Instant;

public record Delivery(
        DeliveryStatus status,
        int attemptCount,
        Instant lastAttemptAt,
        String failureReason
) {
}
