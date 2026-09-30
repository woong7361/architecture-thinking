package com.thinking.tennis.api;

import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AvailabilitySnapshot;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public final class ApiModels {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private ApiModels() {
    }

    public static AvailabilityResponse availability(AvailabilitySnapshot snapshot) {
        List<AvailabilitySlotResponse> slots = snapshot.slots().stream()
                .map(slot -> new AvailabilitySlotResponse(time(slot.startTime()), time(slot.endTime()), slot.available()))
                .toList();
        return new AvailabilityResponse(snapshot.courtId(), snapshot.courtName(), snapshot.date().toString(),
                snapshot.confirmedAt().toString(), snapshot.stale(),
                snapshot.staleReason() == null ? null : snapshot.staleReason().name(),
                snapshot.reservationUrl(), slots);
    }

    public static AlertResponse alert(Alert alert) {
        AlertDeliveryResponse delivery = null;
        if (alert.delivery() != null) {
            delivery = new AlertDeliveryResponse(alert.delivery().status().name(), alert.delivery().attemptCount(),
                    alert.delivery().lastAttemptAt() == null ? null : alert.delivery().lastAttemptAt().toString(),
                    alert.delivery().failureReason());
        }
        return new AlertResponse(alert.alertId().toString(), alert.courtId(), alert.courtName(),
                alert.reservationUrl(), alert.date().toString(),
                new TimeSlotResponse(time(alert.slot().startTime()), time(alert.slot().endTime())),
                alert.status().name(), alert.createdAt().toString(), alert.expiresAt().toString(),
                alert.lastCheckedAt() == null ? null : alert.lastCheckedAt().toString(),
                alert.checkDelayed(), delivery);
    }

    public static AlertListResponse alertList(List<Alert> alerts) {
        return new AlertListResponse(alerts.stream().map(ApiModels::alert).toList());
    }

    public static String time(LocalTime value) {
        return value.format(TIME_FORMATTER);
    }

    public record TimeSlotResponse(String startTime, String endTime) {
    }

    public record AlertDeliveryResponse(
            String status,
            int attemptCount,
            String lastAttemptAt,
            String failureReason
    ) {
    }

    public record AlertResponse(
            String alertId,
            String courtId,
            String courtName,
            String reservationUrl,
            String date,
            TimeSlotResponse slot,
            String status,
            String createdAt,
            String expiresAt,
            String lastCheckedAt,
            boolean checkDelayed,
            AlertDeliveryResponse delivery
    ) {
    }

    public record AlertListResponse(List<AlertResponse> items) {
    }

    public record AvailabilitySlotResponse(String startTime, String endTime, boolean available) {
    }

    public record AvailabilityResponse(
            String courtId,
            String courtName,
            String date,
            String confirmedAt,
            boolean stale,
            String staleReason,
            String reservationUrl,
            List<AvailabilitySlotResponse> slots
    ) {
    }

    public record ProblemSpec(
            String type,
            String title,
            int status,
            String code,
            boolean retryable,
            Integer retryAfterSeconds,
            String detail
    ) {
        public Map<String, Object> body(String instance, String traceId) {
            java.util.LinkedHashMap<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("type", type);
            body.put("title", title);
            body.put("status", status);
            if (detail != null) {
                body.put("detail", detail);
            }
            body.put("instance", instance);
            body.put("code", code);
            body.put("retryable", retryable);
            if (retryAfterSeconds != null) {
                body.put("retryAfterSeconds", retryAfterSeconds);
            }
            if (traceId != null) {
                body.put("traceId", traceId);
            }
            return body;
        }
    }
}
