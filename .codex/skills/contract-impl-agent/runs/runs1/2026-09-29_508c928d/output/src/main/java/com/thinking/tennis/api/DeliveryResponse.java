package com.thinking.tennis.api;

import com.thinking.tennis.domain.DeliveryStatus;

import java.time.Instant;

public record DeliveryResponse(
        DeliveryStatus status,
        int attemptCount,
        Instant lastAttemptAt,
        String failureReason
) {
}
