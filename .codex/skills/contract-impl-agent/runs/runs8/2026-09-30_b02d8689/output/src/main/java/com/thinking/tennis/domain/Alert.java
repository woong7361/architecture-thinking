package com.thinking.tennis.domain;

import com.thinking.tennis.port.CourtAvailabilityPort;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class Alert {
    private static final Duration CHECK_DELAY_THRESHOLD = Duration.ofSeconds(60);

    private final UUID alertId;
    private final String userId;
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
            UUID alertId,
            String userId,
            String courtId,
            String courtName,
            String reservationUrl,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.alertId = alertId;
        this.userId = userId;
        this.courtId = courtId;
        this.courtName = courtName;
        this.reservationUrl = reservationUrl;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = AlertStatus.WATCHING;
    }

    public synchronized boolean expireIfDue(Instant now) {
        if (status == AlertStatus.WATCHING && !now.isBefore(expiresAt)) {
            status = AlertStatus.EXPIRED;
            delivery = null;
            return true;
        }
        return false;
    }

    public synchronized boolean matches(String owner, String requestedCourtId, LocalDate requestedDate,
                                        LocalTime requestedStart, LocalTime requestedEnd, Instant now) {
        expireIfDue(now);
        return userId.equals(owner)
                && courtId.equals(requestedCourtId)
                && date.equals(requestedDate)
                && startTime.equals(requestedStart)
                && endTime.equals(requestedEnd)
                && status == AlertStatus.WATCHING;
    }

    public synchronized void applyAvailability(AvailabilitySnapshot snapshot, Instant now) {
        if (status != AlertStatus.WATCHING) {
            return;
        }
        if (!now.isBefore(expiresAt)) {
            status = AlertStatus.EXPIRED;
            delivery = null;
            return;
        }

        lastCheckedAt = snapshot.confirmedAt();
        checkDelayed = Duration.between(lastCheckedAt, now).compareTo(CHECK_DELAY_THRESHOLD) > 0;
        if (snapshot.stale()) {
            return;
        }

        boolean available = snapshot.slots().stream()
                .anyMatch(slot -> slot.startTime().equals(startTime)
                        && slot.endTime().equals(endTime)
                        && slot.available());
        if (available && delivery == null) {
            delivery = new AlertDelivery(DeliveryStatus.PENDING, 0, null, null);
        } else if (!available && delivery != null && delivery.status() == DeliveryStatus.PENDING) {
            delivery = null;
        }
    }

    public synchronized AlertSnapshot cancel(Instant now) {
        expireIfDue(now);
        if (status == AlertStatus.WATCHING) {
            status = AlertStatus.CANCELED;
            delivery = null;
        }
        return snapshot(now);
    }

    public synchronized AlertSnapshot snapshot(Instant now) {
        boolean delayed = lastCheckedAt == null
                ? Duration.between(createdAt, now).compareTo(CHECK_DELAY_THRESHOLD) > 0
                : checkDelayed;
        return new AlertSnapshot(
                alertId, userId, courtId, courtName, reservationUrl, date, startTime, endTime,
                status, createdAt, expiresAt, lastCheckedAt, delayed, delivery);
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
}
