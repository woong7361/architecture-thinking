package com.thinking.tennis.domain;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;
import java.time.LocalDate;

public final class Alert {
    private final String alertId;
    private final String ownerId;
    private final String courtId;
    private final String courtName;
    private final String reservationUrl;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Instant createdAt;
    private final Instant expiresAt;

    private AlertStatus status;
    private Instant lastCheckedAt;
    private boolean checkDelayed;
    private AlertDelivery delivery;

    public Alert(
            String alertId,
            String ownerId,
            String courtId,
            String courtName,
            String reservationUrl,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.alertId = Objects.requireNonNull(alertId);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.courtId = Objects.requireNonNull(courtId);
        this.courtName = Objects.requireNonNull(courtName);
        this.reservationUrl = Objects.requireNonNull(reservationUrl);
        this.date = Objects.requireNonNull(date);
        this.startTime = Objects.requireNonNull(startTime);
        this.endTime = Objects.requireNonNull(endTime);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.status = AlertStatus.WATCHING;
        this.checkDelayed = false;
    }

    public synchronized void expireIfDue(Instant now) {
        if (status == AlertStatus.WATCHING && !now.isBefore(expiresAt)) {
            status = AlertStatus.EXPIRED;
            delivery = null;
        }
    }

    public synchronized boolean isWatching() {
        return status == AlertStatus.WATCHING;
    }

    public synchronized void recordAvailabilityCheck(Instant checkedAt, Instant now, long delayedAfterSeconds) {
        expireIfDue(now);
        if (status != AlertStatus.WATCHING) {
            return;
        }
        lastCheckedAt = checkedAt;
        checkDelayed = checkedAt.plusSeconds(delayedAfterSeconds).isBefore(now);
    }

    public synchronized void cancel() {
        if (status == AlertStatus.WATCHING) {
            status = AlertStatus.CANCELED;
            if (delivery != null && delivery.status() == DeliveryStatus.PENDING) {
                delivery = null;
            }
        }
    }

    public synchronized void setDelivery(AlertDelivery delivery) {
        if (status != AlertStatus.WATCHING) {
            return;
        }
        this.delivery = delivery;
        if (delivery != null && delivery.status() == DeliveryStatus.SENT) {
            status = AlertStatus.NOTIFIED;
        }
    }

    public String alertId() {
        return alertId;
    }

    public String ownerId() {
        return ownerId;
    }

    public String courtId() {
        return courtId;
    }

    public String courtName() {
        return courtName;
    }

    public String reservationUrl() {
        return reservationUrl;
    }

    public LocalDate date() {
        return date;
    }

    public LocalTime startTime() {
        return startTime;
    }

    public LocalTime endTime() {
        return endTime;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public synchronized AlertStatus status() {
        return status;
    }

    public synchronized Instant lastCheckedAt() {
        return lastCheckedAt;
    }

    public synchronized boolean checkDelayed() {
        return checkDelayed;
    }

    public synchronized AlertDelivery delivery() {
        return delivery;
    }
}
