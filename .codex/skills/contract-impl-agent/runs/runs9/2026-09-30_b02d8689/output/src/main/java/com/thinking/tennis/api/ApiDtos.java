package com.thinking.tennis.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertDelivery;
import com.thinking.tennis.domain.AvailabilitySlot;
import com.thinking.tennis.domain.CourtAvailabilitySnapshot;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public final class ApiDtos {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private ApiDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record AlertRequestDto(String courtId, LocalDate date, TimeSlotDto slot) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record TimeSlotDto(String startTime, String endTime) {
    }

    public record AlertDto(UUID alertId,
                    String courtId,
                    String courtName,
                    String reservationUrl,
                    LocalDate date,
                    TimeSlotDto slot,
                    String status,
                    Instant createdAt,
                    Instant expiresAt,
                    Instant lastCheckedAt,
                    boolean checkDelayed,
                    DeliveryDto delivery) {
        static AlertDto from(Alert alert, boolean checkDelayed) {
            return new AlertDto(
                    alert.alertId(),
                    alert.courtId(),
                    alert.courtName(),
                    alert.reservationUrl(),
                    alert.date(),
                    new TimeSlotDto(TIME_FORMATTER.format(alert.slot().startTime()), TIME_FORMATTER.format(alert.slot().endTime())),
                    alert.status().name(),
                    alert.createdAt(),
                    alert.expiresAt(),
                    alert.lastCheckedAt(),
                    checkDelayed,
                    DeliveryDto.from(alert.delivery()));
        }
    }

    public record DeliveryDto(String status, int attemptCount, Instant lastAttemptAt, String failureReason) {
        static DeliveryDto from(AlertDelivery delivery) {
            if (delivery == null) {
                return null;
            }
            return new DeliveryDto(delivery.status().name(), delivery.attemptCount(), delivery.lastAttemptAt(), delivery.failureReason());
        }
    }

    public record AlertListDto(List<AlertDto> items) {
    }

    public record CourtAvailabilityDto(String courtId,
                                String courtName,
                                LocalDate date,
                                Instant confirmedAt,
                                boolean stale,
                                String staleReason,
                                String reservationUrl,
                                List<AvailabilitySlotDto> slots) {
        static CourtAvailabilityDto from(CourtAvailabilitySnapshot snapshot) {
            return new CourtAvailabilityDto(
                    snapshot.courtId(),
                    snapshot.courtName(),
                    snapshot.date(),
                    snapshot.confirmedAt(),
                    snapshot.stale(),
                    snapshot.staleReason(),
                    snapshot.reservationUrl(),
                    snapshot.slots().stream().map(AvailabilitySlotDto::from).toList());
        }
    }

    public record AvailabilitySlotDto(String startTime, String endTime, boolean available) {
        static AvailabilitySlotDto from(AvailabilitySlot slot) {
            return new AvailabilitySlotDto(
                    TIME_FORMATTER.format(slot.slot().startTime()),
                    TIME_FORMATTER.format(slot.slot().endTime()),
                    slot.available());
        }
    }
}
