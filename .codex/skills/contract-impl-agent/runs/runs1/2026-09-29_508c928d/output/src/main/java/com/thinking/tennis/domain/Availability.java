package com.thinking.tennis.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record Availability(
        String courtId,
        String courtName,
        String reservationUrl,
        LocalDate date,
        Instant confirmedAt,
        List<Slot> slots
) {
    public Availability {
        slots = List.copyOf(slots);
    }

    public record Slot(LocalTime startTime, LocalTime endTime, boolean available) {
    }
}
