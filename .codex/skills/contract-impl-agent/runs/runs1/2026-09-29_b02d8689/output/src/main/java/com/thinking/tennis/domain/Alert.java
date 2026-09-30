package com.thinking.tennis.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Alert(
        UUID alertId,
        UserId owner,
        String courtId,
        String courtName,
        String reservationUrl,
        LocalDate date,
        TimeWindow slot,
        AlertStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant lastCheckedAt,
        boolean checkDelayed,
        AlertDelivery delivery
) {

    public Alert withStatus(AlertStatus nextStatus) {
        return new Alert(alertId, owner, courtId, courtName, reservationUrl, date, slot, nextStatus,
                createdAt, expiresAt, lastCheckedAt, checkDelayed, delivery);
    }

    public Alert withDelivery(AlertDelivery nextDelivery) {
        return new Alert(alertId, owner, courtId, courtName, reservationUrl, date, slot, status,
                createdAt, expiresAt, lastCheckedAt, checkDelayed, nextDelivery);
    }

    public Alert withLastCheckedAt(Instant checkedAt) {
        return new Alert(alertId, owner, courtId, courtName, reservationUrl, date, slot, status,
                createdAt, expiresAt, checkedAt, checkDelayed, delivery);
    }

    public Alert withCheckDelayed(boolean delayed) {
        return new Alert(alertId, owner, courtId, courtName, reservationUrl, date, slot, status,
                createdAt, expiresAt, lastCheckedAt, delayed, delivery);
    }

    public boolean isWatching() {
        return status == AlertStatus.WATCHING;
    }

    public enum AlertStatus {
        WATCHING,
        NOTIFIED,
        CANCELED,
        EXPIRED
    }

    public enum DeliveryStatus {
        PENDING,
        SENT,
        FAILED
    }

    public record AlertDelivery(
            DeliveryStatus status,
            int attemptCount,
            Instant lastAttemptAt,
            String failureReason
    ) {
    }
}
