package com.thinking.tennis.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AvailabilitySnapshot(String courtId,
                                   String courtName,
                                   String reservationUrl,
                                   LocalDate date,
                                   Instant confirmedAt,
                                   boolean stale,
                                   String staleReason,
                                   List<Slot> slots) {

    public AvailabilitySnapshot {
        slots = List.copyOf(slots);
    }

    public AvailabilitySnapshot stale(String reason) {
        return new AvailabilitySnapshot(courtId, courtName, reservationUrl, date, confirmedAt, true, reason, slots);
    }

    public record Slot(LocalTime startTime, LocalTime endTime, boolean available) {
    }
}
