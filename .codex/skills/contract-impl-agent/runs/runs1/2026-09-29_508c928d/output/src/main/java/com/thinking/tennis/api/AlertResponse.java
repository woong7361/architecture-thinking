package com.thinking.tennis.api;

import com.thinking.tennis.domain.AlertStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AlertResponse(
        UUID alertId,
        String courtId,
        String courtName,
        String reservationUrl,
        LocalDate date,
        TimeSlotResponse slot,
        AlertStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant lastCheckedAt,
        boolean checkDelayed,
        DeliveryResponse delivery
) {
}
