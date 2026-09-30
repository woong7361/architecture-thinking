package com.thinking.tennis.api;

import com.thinking.tennis.domain.AlertDelivery;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.AvailabilitySnapshot;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public final class ApiModels {
    private ApiModels() {
    }

    public record AlertRequest(
            String courtId,
            @Schema(format = "date") String date,
            TimeSlot slot
    ) {
    }

    public record TimeSlot(
            @Schema(pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$") String startTime,
            @Schema(pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$") String endTime
    ) {
    }

    public record AlertResponse(
            String alertId,
            String courtId,
            String courtName,
            String reservationUrl,
            @Schema(format = "date") String date,
            TimeSlot slot,
            AlertStatus status,
            Instant createdAt,
            Instant expiresAt,
            Instant lastCheckedAt,
            boolean checkDelayed,
            AlertDelivery delivery
    ) {
    }

    public record AlertList(List<AlertResponse> items) {
    }

    public record AvailabilityResponse(
            String courtId,
            String courtName,
            @Schema(format = "date") String date,
            Instant confirmedAt,
            boolean stale,
            String staleReason,
            String reservationUrl,
            List<AvailabilitySlotResponse> slots
    ) {
    }

    public record AvailabilitySlotResponse(
            @Schema(pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$") String startTime,
            @Schema(pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$") String endTime,
            boolean available
    ) {
    }

    public record Problem(
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
