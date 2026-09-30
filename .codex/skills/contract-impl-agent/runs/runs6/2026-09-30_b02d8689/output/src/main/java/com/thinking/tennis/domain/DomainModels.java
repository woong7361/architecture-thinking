package com.thinking.tennis.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class DomainModels {

    private DomainModels() {
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

    public enum ErrorCode {
        VALIDATION_FAILED,
        UNAUTHENTICATED,
        COURT_NOT_SUPPORTED,
        SLOT_NOT_SUPPORTED,
        ALERT_WINDOW_CLOSED,
        ALERT_NOT_FOUND,
        IDEMPOTENCY_KEY_REUSED,
        UPSTREAM_TIMEOUT,
        UPSTREAM_UNAVAILABLE,
        UPSTREAM_RESPONSE_UNREADABLE,
        STORAGE_TIMEOUT,
        CONCURRENT_UPDATE_CONFLICT,
        INTERNAL_ERROR
    }

    public record TimeSlot(LocalTime startTime, LocalTime endTime) {
    }

    public record AlertDelivery(DeliveryStatus status,
                                int attemptCount,
                                Instant lastAttemptAt,
                                String failureReason) {
    }

    public record Alert(UUID alertId,
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

        public Alert withStatus(AlertStatus nextStatus) {
            return new Alert(alertId, userId, courtId, courtName, reservationUrl, date, slot,
                    nextStatus, createdAt, expiresAt, lastCheckedAt, delivery);
        }

        public Alert withLastCheckedAt(Instant checkedAt) {
            return new Alert(alertId, userId, courtId, courtName, reservationUrl, date, slot,
                    status, createdAt, expiresAt, checkedAt, delivery);
        }

        public Alert withDelivery(AlertDelivery nextDelivery) {
            return new Alert(alertId, userId, courtId, courtName, reservationUrl, date, slot,
                    status, createdAt, expiresAt, lastCheckedAt, nextDelivery);
        }

        public Alert cancelPendingDelivery() {
            return withDelivery(null);
        }
    }

    public record AvailabilitySlot(LocalTime startTime, LocalTime endTime, boolean available) {
    }

    public record CourtAvailability(String courtId,
                                    String courtName,
                                    LocalDate date,
                                    Instant confirmedAt,
                                    boolean stale,
                                    String staleReason,
                                    String reservationUrl,
                                    List<AvailabilitySlot> slots) {
    }
}
