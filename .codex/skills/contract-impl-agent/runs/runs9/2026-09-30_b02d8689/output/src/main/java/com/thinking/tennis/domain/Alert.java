package com.thinking.tennis.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class Alert {

    private final UUID alertId;
    private final String userId;
    private final String courtId;
    private final String courtName;
    private final String reservationUrl;
    private final LocalDate date;
    private final TimeSlot slot;
    private final Instant createdAt;
    private Instant expiresAt;
    private Instant lastCheckedAt;
    private AlertStatus status;
    private AlertDelivery delivery;

    public Alert(UUID alertId,
                 String userId,
                 String courtId,
                 String courtName,
                 String reservationUrl,
                 LocalDate date,
                 TimeSlot slot,
                 AlertStatus status,
                 Instant createdAt,
                 Instant expiresAt,
                 Instant lastCheckedAt,
                 AlertDelivery delivery) {
        this.alertId = alertId;
        this.userId = userId;
        this.courtId = courtId;
        this.courtName = courtName;
        this.reservationUrl = reservationUrl;
        this.date = date;
        this.slot = slot;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.lastCheckedAt = lastCheckedAt;
        this.delivery = delivery;
    }

    public UUID alertId() {
        return alertId;
    }

    public String userId() {
        return userId;
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

    public TimeSlot slot() {
        return slot;
    }

    public AlertStatus status() {
        return status;
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

    public AlertDelivery delivery() {
        return delivery;
    }

    public boolean isWatching() {
        return status == AlertStatus.WATCHING;
    }

    public void markChecked(Instant checkedAt) {
        this.lastCheckedAt = checkedAt;
    }

    public void markExpired() {
        if (status == AlertStatus.WATCHING) {
            status = AlertStatus.EXPIRED;
            delivery = null;
        }
    }

    public void cancel() {
        if (status == AlertStatus.WATCHING) {
            status = AlertStatus.CANCELED;
            delivery = null;
        }
    }

    public void queueDelivery() {
        if (status == AlertStatus.WATCHING && delivery == null) {
            delivery = new AlertDelivery(DeliveryStatus.PENDING, 0, null, null);
        }
    }

    public void clearPendingDelivery() {
        if (delivery != null && delivery.status() == DeliveryStatus.PENDING) {
            delivery = null;
        }
    }
}
