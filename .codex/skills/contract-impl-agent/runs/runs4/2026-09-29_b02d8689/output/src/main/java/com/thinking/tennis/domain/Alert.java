package com.thinking.tennis.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

public record Alert(
        UUID alertId,
        String ownerId,
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
        AlertDelivery delivery) {

    public Alert {
        Objects.requireNonNull(alertId);
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(courtId);
        Objects.requireNonNull(courtName);
        Objects.requireNonNull(reservationUrl);
        Objects.requireNonNull(date);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endTime);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(expiresAt);
    }

    public Alert withStatus(AlertStatus nextStatus) {
        return new Alert(alertId, ownerId, courtId, courtName, reservationUrl, date,
                startTime, endTime, nextStatus, createdAt, expiresAt, lastCheckedAt,
                checkDelayed, delivery);
    }

    public Alert withCheckState(Instant checkedAt, boolean delayed) {
        return new Alert(alertId, ownerId, courtId, courtName, reservationUrl, date,
                startTime, endTime, status, createdAt, expiresAt, checkedAt,
                delayed, delivery);
    }
}
