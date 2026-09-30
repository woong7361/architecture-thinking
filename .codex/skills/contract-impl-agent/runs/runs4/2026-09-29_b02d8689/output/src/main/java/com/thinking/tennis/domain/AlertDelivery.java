package com.thinking.tennis.domain;

import java.time.Instant;

public record AlertDelivery(
        DeliveryStatus status,
        int attemptCount,
        Instant lastAttemptAt,
        String failureReason) {
}
