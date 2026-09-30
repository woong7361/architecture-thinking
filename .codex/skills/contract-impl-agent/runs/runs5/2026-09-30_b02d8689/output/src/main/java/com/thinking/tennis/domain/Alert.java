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
    private Instant lastCheckedAt;
    private AlertStatus status;
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
                 Instant createdAt,
                 Instant expiresAt) {
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

    public UUID alertId() {
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

    public Instant lastCheckedAt() {
        return lastCheckedAt;
    }

    public AlertStatus status() {
        return status;
    }

    public boolean checkDelayed() {
        return checkDelayed;
    }

    public AlertDelivery delivery() {
        return delivery;
    }

    public void updateExpiry(Instant expiresAt) {
        this.expiresAt = Objects.requireNonNull(expiresAt);
    }

    public void markChecked(Instant checkedAt, boolean delayed) {
        this.lastCheckedAt = Objects.requireNonNull(checkedAt);
        this.checkDelayed = delayed;
    }

    public void markDelayed(boolean delayed) {
        this.checkDelayed = delayed;
    }

    public void cancel() {
        if (status == AlertStatus.WATCHING) {
            status = AlertStatus.CANCELED;
            delivery = null;
        }
    }

    public void expire() {
        if (status == AlertStatus.WATCHING) {
            status = AlertStatus.EXPIRED;
            delivery = null;
        }
    }

    public void queueDelivery() {
        if (status == AlertStatus.WATCHING && delivery == null) {
            delivery = new AlertDelivery(DeliveryStatus.PENDING, 0, null, null);
        }
    }

    public void clearPendingDelivery() {
        if (status == AlertStatus.WATCHING
                && delivery != null
                && delivery.status() == DeliveryStatus.PENDING) {
            delivery = null;
        }
    }

    public boolean hasTerminalDeliveryFailure() {
        return delivery != null && delivery.status() == DeliveryStatus.FAILED;
    }

    public boolean matches(String courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return this.courtId.equals(courtId)
                && this.date.equals(date)
                && this.startTime.equals(startTime)
                && this.endTime.equals(endTime);
    }

    public Alert copy() {
        Alert copy = new Alert(
                alertId,
                ownerId,
                courtId,
                courtName,
                reservationUrl,
                date,
                startTime,
                endTime,
                createdAt,
                expiresAt
        );
        copy.lastCheckedAt = lastCheckedAt;
        copy.status = status;
        copy.checkDelayed = checkDelayed;
        copy.delivery = delivery;
        return copy;
    }
}
