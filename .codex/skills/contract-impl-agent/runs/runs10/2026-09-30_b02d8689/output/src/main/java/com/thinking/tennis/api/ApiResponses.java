package com.thinking.tennis.api;

import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertDelivery;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.AvailabilitySnapshot;
import com.thinking.tennis.domain.DeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class ApiResponses {
    private ApiResponses() {
    }

    public static AlertResponse alert(Alert alert) {
        return new AlertResponse(alert.alertId().toString(), alert.courtId(), alert.courtName(), alert.reservationUrl(),
                alert.date(), new TimeSlotResponse(formatTime(alert.startTime()), formatTime(alert.endTime())), alert.status(),
                alert.createdAt(), alert.expiresAt(), alert.lastCheckedAt(), alert.checkDelayed(),
                alert.delivery() == null ? null : delivery(alert.delivery()));
    }

    public static AlertDeliveryResponse delivery(AlertDelivery delivery) {
        return new AlertDeliveryResponse(delivery.status(), delivery.attemptCount(), delivery.lastAttemptAt(),
                delivery.failureReason());
    }

    public static AvailabilityResponse availability(AvailabilitySnapshot snapshot) {
        List<AvailabilitySlotResponse> slots = snapshot.slots().stream()
                .map(slot -> new AvailabilitySlotResponse(formatTime(slot.startTime()), formatTime(slot.endTime()), slot.available()))
                .toList();
        return new AvailabilityResponse(snapshot.courtId(), snapshot.courtName(), snapshot.date(),
                snapshot.confirmedAt(), snapshot.stale(), snapshot.staleReason(), snapshot.reservationUrl(), slots);
    }

    public static AlertListResponse alertList(List<Alert> alerts) {
        return new AlertListResponse(alerts.stream().map(ApiResponses::alert).toList());
    }

    @Schema(name = "TimeSlot", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record TimeSlotResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
            String startTime,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
            String endTime) {
    }

    @Schema(name = "AlertDelivery", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record AlertDeliveryResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            DeliveryStatus status,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0", maximum = "4", format = "")
            int attemptCount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, format = "date-time")
            Instant lastAttemptAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
            String failureReason) {
    }

    @Schema(name = "Alert", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record AlertResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
            String alertId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                    minLength = 1,
                    maxLength = 64,
                    pattern = "^[a-z0-9][a-z0-9-]*$")
            String courtId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
            String courtName,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uri")
            String reservationUrl,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
            LocalDate date,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            TimeSlotResponse slot,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            AlertStatus status,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
            Instant createdAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
            Instant expiresAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, format = "date-time")
            Instant lastCheckedAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            boolean checkDelayed,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
            AlertDeliveryResponse delivery) {
    }

    @Schema(name = "AlertList", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record AlertListResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            List<AlertResponse> items) {
    }

    @Schema(name = "AvailabilitySlot", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record AvailabilitySlotResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
            String startTime,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
            String endTime,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            boolean available) {
    }

    @Schema(name = "CourtAvailability", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record AvailabilityResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                    minLength = 1,
                    maxLength = 64,
                    pattern = "^[a-z0-9][a-z0-9-]*$")
            String courtId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
            String courtName,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
            LocalDate date,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
            Instant confirmedAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            boolean stale,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
                    allowableValues = {"UPSTREAM_TIMEOUT", "UPSTREAM_UNAVAILABLE", "UPSTREAM_RESPONSE_UNREADABLE"})
            String staleReason,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uri")
            String reservationUrl,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            List<AvailabilitySlotResponse> slots) {
    }

    private static String formatTime(LocalTime value) {
        return value.toString();
    }
}
