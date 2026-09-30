package com.thinking.tennis.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class Alert {
    private static final Duration CHECK_INTERVAL = Duration.ofSeconds(20);

    private final UUID alertId;
    private final String userId;
    private final String courtId;
    private final String courtName;
    private final String reservationUrl;
    private final LocalDate date;
    private final TimeSlot slot;
    private final Instant createdAt;
    private final Instant expiresAt;
    private AlertStatus status;
    private Instant lastCheckedAt;
    private Delivery delivery;

    public Alert(
            UUID alertId,
            String userId,
            String courtId,
            String courtName,
            String reservationUrl,
            LocalDate date,
            TimeSlot slot,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.alertId = alertId;
        this.userId = userId;
        this.courtId = courtId;
        this.courtName = courtName;
        this.reservationUrl = reservationUrl;
        this.date = date;
        this.slot = slot;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = AlertStatus.WATCHING;
    }

    public void cancel() {
        if (status == AlertStatus.WATCHING) {
            status = AlertStatus.CANCELED;
            if (delivery == null || delivery.status() == DeliveryStatus.PENDING) {
                delivery = null;
            }
        }
    }

    public void expire(Instant now) {
        if (status == AlertStatus.WATCHING && !expiresAt.isAfter(now)) {
            status = AlertStatus.EXPIRED;
            if (delivery == null || delivery.status() == DeliveryStatus.PENDING) {
                delivery = null;
            }
        }
    }

    public AlertSnapshot snapshot(Instant now) {
        Instant checkReference = lastCheckedAt == null ? createdAt : lastCheckedAt;
        boolean checkDelayed = checkReference.plus(CHECK_INTERVAL.multipliedBy(3)).isBefore(now);
        return new AlertSnapshot(
                alertId,
                courtId,
                courtName,
                reservationUrl,
                date,
                slot,
                status,
                createdAt,
                expiresAt,
                lastCheckedAt,
                checkDelayed,
                delivery
        );
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

    public LocalDate date() {
        return date;
    }

    public TimeSlot slot() {
        return slot;
    }
}
