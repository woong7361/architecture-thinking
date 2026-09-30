package com.thinking.tennis.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

public final class Alert {
    private final UUID alertId;
    private final String ownerId;
    private final String courtId;
    private final String courtName;
    private final String reservationUrl;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Instant createdAt;
    private Instant expiresAt;
    private AlertStatus status;
    private Instant lastCheckedAt;
    private boolean checkDelayed;
    private AlertDelivery delivery;

    public Alert(UUID alertId,
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
        this.alertId = alertId;
        this.ownerId = ownerId;
        this.courtId = courtId;
        this.courtName = courtName;
        this.reservationUrl = reservationUrl;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.lastCheckedAt = lastCheckedAt;
        this.checkDelayed = checkDelayed;
        this.delivery = delivery;
    }

    public static Alert watching(UUID alertId,
                                 String ownerId,
                                 String courtId,
                                 String courtName,
                                 String reservationUrl,
                                 LocalDate date,
                                 LocalTime startTime,
                                 LocalTime endTime,
                                 Instant createdAt,
                                 Instant expiresAt) {
        return new Alert(alertId, ownerId, courtId, courtName, reservationUrl, date, startTime, endTime,
                AlertStatus.WATCHING, createdAt, expiresAt, null, false, null);
    }

    public void recordSuccessfulCheck(Instant confirmedAt, boolean available) {
        if (status != AlertStatus.WATCHING) {
            return;
        }
        lastCheckedAt = confirmedAt;
        checkDelayed = false;
        if (available && delivery == null) {
            delivery = new AlertDelivery(DeliveryStatus.PENDING, 0, null, null);
        } else if (!available && delivery != null && delivery.status() == DeliveryStatus.PENDING) {
            delivery = null;
        }
    }

    public void markNotified(Instant attemptedAt) {
        if (status == AlertStatus.WATCHING && delivery != null) {
            delivery = new AlertDelivery(DeliveryStatus.SENT, Math.max(1, delivery.attemptCount()), attemptedAt, null);
            status = AlertStatus.NOTIFIED;
            checkDelayed = false;
        }
    }

    public void markDeliveryFailed(Instant attemptedAt, String reason) {
        if (status == AlertStatus.WATCHING && delivery != null) {
            delivery = new AlertDelivery(DeliveryStatus.FAILED, delivery.attemptCount(), attemptedAt, reason);
            checkDelayed = false;
        }
    }

    public void refreshDelayed(Instant now, long delaySeconds) {
        if (status != AlertStatus.WATCHING) {
            checkDelayed = false;
            return;
        }
        Instant baseline = lastCheckedAt == null ? createdAt : lastCheckedAt;
        checkDelayed = now.isAfter(baseline.plusSeconds(delaySeconds));
    }

    public void cancel() {
        if (!isTerminal()) {
            status = AlertStatus.CANCELED;
            delivery = null;
            checkDelayed = false;
        }
    }

    public void expire() {
        if (status == AlertStatus.WATCHING) {
            status = AlertStatus.EXPIRED;
            delivery = null;
            checkDelayed = false;
        }
    }

    public boolean isDue(Instant now) {
        return status == AlertStatus.WATCHING && !now.isBefore(expiresAt);
    }

    public boolean isTerminal() {
        return status != AlertStatus.WATCHING;
    }

    public boolean hasCondition(String ownerId, String courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return Objects.equals(this.ownerId, ownerId)
                && Objects.equals(this.courtId, courtId)
                && Objects.equals(this.date, date)
                && Objects.equals(this.startTime, startTime)
                && Objects.equals(this.endTime, endTime);
    }

    public Alert copy() {
        return new Alert(alertId, ownerId, courtId, courtName, reservationUrl, date, startTime, endTime, status,
                createdAt, expiresAt, lastCheckedAt, checkDelayed, delivery == null ? null : delivery.copy());
    }

    public UUID alertId() { return alertId; }
    public String ownerId() { return ownerId; }
    public String courtId() { return courtId; }
    public String courtName() { return courtName; }
    public String reservationUrl() { return reservationUrl; }
    public LocalDate date() { return date; }
    public LocalTime startTime() { return startTime; }
    public LocalTime endTime() { return endTime; }
    public AlertStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant expiresAt() { return expiresAt; }
    public Instant lastCheckedAt() { return lastCheckedAt; }
    public boolean checkDelayed() { return checkDelayed; }
    public AlertDelivery delivery() { return delivery; }
}
