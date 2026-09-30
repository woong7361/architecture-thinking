package com.thinking.tennis.api;

import com.thinking.tennis.app.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ApiModels {

    private ApiModels() {
    }

    @Schema(name = "AlertRequest", requiredProperties = {"courtId", "date", "slot"})
    public record AlertRequest(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String courtId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date") LocalDate date,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TimeSlotRequest slot) {
    }

    @Schema(name = "TimeSlot", requiredProperties = {"startTime", "endTime"})
    public record TimeSlotRequest(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
            String startTime,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
            String endTime) {
    }

    @Schema(name = "Alert", requiredProperties = {
            "alertId", "courtId", "courtName", "reservationUrl", "date", "slot", "status",
            "createdAt", "expiresAt", "lastCheckedAt", "checkDelayed", "delivery"
    })
    public record AlertResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") String alertId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String courtId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String courtName,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uri") String reservationUrl,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date") LocalDate date,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TimeSlotRequest slot,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {
                    "WATCHING", "NOTIFIED", "CANCELED", "EXPIRED"
            }) String status,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant createdAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant expiresAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, format = "date-time")
            Instant lastCheckedAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean checkDelayed,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) DeliveryResponse delivery) {
    }

    @Schema(name = "AlertDelivery", requiredProperties = {
            "status", "attemptCount", "lastAttemptAt", "failureReason"
    })
    public record DeliveryResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                    allowableValues = {"PENDING", "SENT", "FAILED"}) String status,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0", maximum = "4") int attemptCount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, format = "date-time")
            Instant lastAttemptAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String failureReason) {
    }

    @Schema(name = "AlertList", requiredProperties = {"items"})
    public record AlertListResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<AlertResponse> items) {
    }

    @Schema(name = "AvailabilitySlot", requiredProperties = {"startTime", "endTime", "available"})
    public record AvailabilitySlotResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String startTime,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String endTime,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean available) {
    }

    @Schema(name = "CourtAvailability", requiredProperties = {
            "courtId", "courtName", "date", "confirmedAt", "stale", "staleReason", "reservationUrl", "slots"
    })
    public record CourtAvailabilityResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String courtId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String courtName,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date") LocalDate date,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant confirmedAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean stale,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String staleReason,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uri") String reservationUrl,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<AvailabilitySlotResponse> slots) {
    }

    @Schema(name = "Problem", requiredProperties = {
            "type", "title", "status", "code", "retryable"
    })
    public record Problem(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uri") String type,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int status,
            @Schema(nullable = true) String detail,
            @Schema(nullable = true, format = "uri-reference") String instance,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ErrorCode code,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean retryable,
            @Schema(nullable = true, minimum = "1") Integer retryAfterSeconds,
            @Schema(nullable = true) String traceId) {
    }
}
