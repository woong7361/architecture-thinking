package com.thinking.tennis.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AlertSnapshot(
        UUID alertId,
        String userId,
        String courtId,
        String courtName,
        String reservationUrl,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        AlertStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant lastCheckedAt,
        boolean checkDelayed,
        AlertDelivery delivery
) {
}
