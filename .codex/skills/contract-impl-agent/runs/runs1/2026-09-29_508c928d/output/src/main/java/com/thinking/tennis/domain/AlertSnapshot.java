package com.thinking.tennis.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AlertSnapshot(
        UUID alertId,
        String courtId,
        String courtName,
        String reservationUrl,
        LocalDate date,
        TimeSlot slot,
        AlertStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant lastCheckedAt,
        boolean checkDelayed,
        Delivery delivery
) {
}
