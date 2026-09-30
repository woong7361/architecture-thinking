package com.thinking.tennis.api;

import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.DeliveryStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ApiModels {

    private ApiModels() {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record TimeSlotRequest(String startTime, String endTime) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record AlertRequest(String courtId, LocalDate date, TimeSlotRequest slot) {
    }

    public record TimeSlotResponse(String startTime, String endTime) {
    }

    public record AlertDeliveryResponse(
            DeliveryStatus status,
            int attemptCount,
            Instant lastAttemptAt,
            String failureReason
    ) {
    }

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
            AlertDeliveryResponse delivery
    ) {
    }

    public record AlertListResponse(List<AlertResponse> items) {
        public AlertListResponse {
            items = List.copyOf(items);
        }
    }

    public record AvailabilitySlotResponse(
            String startTime,
            String endTime,
            boolean available
    ) {
    }

    public record CourtAvailabilityResponse(
            String courtId,
            String courtName,
            LocalDate date,
            Instant confirmedAt,
            boolean stale,
            String staleReason,
            String reservationUrl,
            List<AvailabilitySlotResponse> slots
    ) {
        public CourtAvailabilityResponse {
            slots = List.copyOf(slots);
        }
    }

    public record ProblemResponse(
            String type,
            String title,
            int status,
            String detail,
            String instance,
            String code,
            boolean retryable,
            Integer retryAfterSeconds,
            String traceId
    ) {
    }
}
