package com.thinking.tennis.api;

import com.thinking.tennis.domain.AlertSnapshot;
import com.thinking.tennis.domain.AvailabilitySnapshot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;

public final class ApiMapper {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    private ApiMapper() {
    }

    public static String userId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }

    public static LocalDate date(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new com.thinking.tennis.app.ValidationFailure("date must use ISO-8601 date format.");
        }
    }

    public static LocalTime time(String value, String field) {
        try {
            LocalTime parsed = LocalTime.parse(value, TIME_FORMAT);
            if (parsed.getSecond() != 0 || parsed.getNano() != 0) {
                throw new DateTimeParseException("seconds are not supported", value, 0);
            }
            return parsed;
        } catch (DateTimeParseException exception) {
            throw new com.thinking.tennis.app.ValidationFailure(field + " must use HH:mm format.");
        }
    }

    public static UUID uuid(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new com.thinking.tennis.app.ValidationFailure(field + " must be a UUID.");
        }
    }

    public static ApiModels.AlertResponse alert(AlertSnapshot snapshot) {
        return new ApiModels.AlertResponse(
                snapshot.alertId().toString(),
                snapshot.courtId(),
                snapshot.courtName(),
                snapshot.reservationUrl(),
                snapshot.date().toString(),
                new ApiModels.TimeSlot(format(snapshot.startTime()), format(snapshot.endTime())),
                snapshot.status(),
                snapshot.createdAt(),
                snapshot.expiresAt(),
                snapshot.lastCheckedAt(),
                snapshot.checkDelayed(),
                snapshot.delivery());
    }

    public static ApiModels.AvailabilityResponse availability(AvailabilitySnapshot snapshot) {
        return new ApiModels.AvailabilityResponse(
                snapshot.court().courtId(),
                snapshot.court().courtName(),
                snapshot.date().toString(),
                snapshot.confirmedAt(),
                snapshot.stale(),
                snapshot.staleReason() == null ? null : snapshot.staleReason().name(),
                snapshot.court().reservationUrl(),
                snapshot.slots().stream()
                        .map(slot -> new ApiModels.AvailabilitySlotResponse(
                                format(slot.startTime()), format(slot.endTime()), slot.available()))
                        .toList());
    }

    private static String format(LocalTime value) {
        return TIME_FORMAT.format(value);
    }
}
