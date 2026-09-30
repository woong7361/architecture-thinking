package com.thinking.tennis.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thinking.tennis.app.AlertService;
import com.thinking.tennis.app.FailureCode;
import com.thinking.tennis.domain.AlertDelivery;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.AvailabilitySnapshot;
import com.thinking.tennis.port.CourtAvailabilityPort;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record AlertRequest(
            @JsonProperty(required = true)
            String courtId,
            @JsonProperty(required = true)
            String date,
            @JsonProperty(required = true)
            TimeSlotRequest slot
    ) {
    }

    public record TimeSlotRequest(
            @JsonProperty(required = true)
            String startTime,
            @JsonProperty(required = true)
            String endTime
    ) {
    }

    public record AlertResponse(
            @JsonProperty(required = true)
            String alertId,
            @JsonProperty(required = true)
            String courtId,
            @JsonProperty(required = true)
            String courtName,
            @JsonProperty(required = true)
            String reservationUrl,
            @JsonProperty(required = true)
            LocalDate date,
            @JsonProperty(required = true)
            TimeSlotResponse slot,
            @JsonProperty(required = true)
            AlertStatus status,
            @JsonProperty(required = true)
            Instant createdAt,
            @JsonProperty(required = true)
            Instant expiresAt,
            Instant lastCheckedAt,
            @JsonProperty(required = true)
            boolean checkDelayed,
            @JsonProperty(required = true)
            DeliveryResponse delivery
    ) {
        public static AlertResponse from(AlertService.AlertView alert) {
            return new AlertResponse(
                    alert.alertId(),
                    alert.courtId(),
                    alert.courtName(),
                    alert.reservationUrl(),
                    alert.date(),
                    new TimeSlotResponse(alert.startTime(), alert.endTime()),
                    alert.status(),
                    alert.createdAt(),
                    alert.expiresAt(),
                    alert.lastCheckedAt(),
                    alert.checkDelayed(),
                    DeliveryResponse.from(alert.delivery())
            );
        }
    }

    public record TimeSlotResponse(
            @JsonProperty(required = true)
            LocalTime startTime,
            @JsonProperty(required = true)
            LocalTime endTime
    ) {
    }

    public record DeliveryResponse(
            @JsonProperty(required = true)
            String status,
            @JsonProperty(required = true)
            int attemptCount,
            @JsonProperty(required = true)
            Instant lastAttemptAt,
            @JsonProperty(required = true)
            String failureReason
    ) {
        private static DeliveryResponse from(AlertDelivery delivery) {
            if (delivery == null) {
                return null;
            }
            return new DeliveryResponse(
                    delivery.status().name(),
                    delivery.attemptCount(),
                    delivery.lastAttemptAt(),
                    delivery.failureReason()
            );
        }
    }

    public record AlertListResponse(
            @JsonProperty(required = true)
            List<AlertResponse> items
    ) {
    }

    public record AvailabilityResponse(
            @JsonProperty(required = true)
            String courtId,
            @JsonProperty(required = true)
            String courtName,
            @JsonProperty(required = true)
            LocalDate date,
            @JsonProperty(required = true)
            Instant confirmedAt,
            @JsonProperty(required = true)
            boolean stale,
            @JsonProperty(required = true)
            CourtAvailabilityPort.UpstreamFailureReason staleReason,
            @JsonProperty(required = true)
            String reservationUrl,
            @JsonProperty(required = true)
            List<AvailabilitySlotResponse> slots
    ) {
        public static AvailabilityResponse from(
                AvailabilitySnapshot snapshot,
                boolean stale,
                CourtAvailabilityPort.UpstreamFailureReason staleReason
        ) {
            return new AvailabilityResponse(
                    snapshot.court().courtId(),
                    snapshot.court().courtName(),
                    snapshot.date(),
                    snapshot.confirmedAt(),
                    stale,
                    staleReason,
                    snapshot.court().reservationUrl(),
                    snapshot.slots().stream().map(slot -> new AvailabilitySlotResponse(
                            slot.startTime(),
                            slot.endTime(),
                            slot.available()
                    )).toList()
            );
        }
    }

    public record AvailabilitySlotResponse(
            @JsonProperty(required = true)
            LocalTime startTime,
            @JsonProperty(required = true)
            LocalTime endTime,
            @JsonProperty(required = true)
            boolean available
    ) {
    }

    public record ProblemResponse(
            @JsonProperty(required = true)
            String type,
            @JsonProperty(required = true)
            String title,
            @JsonProperty(required = true)
            int status,
            String detail,
            String instance,
            @JsonProperty(required = true)
            FailureCode code,
            @JsonProperty(required = true)
            boolean retryable,
            Integer retryAfterSeconds,
            String traceId
    ) {
    }
}
