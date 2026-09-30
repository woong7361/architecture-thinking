package com.thinking.tennis.domain;

import com.thinking.tennis.port.CourtAvailabilityPort;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AvailabilitySnapshot(
        String courtId,
        String courtName,
        LocalDate date,
        Instant confirmedAt,
        boolean stale,
        CourtAvailabilityPort.UpstreamFailureReason staleReason,
        String reservationUrl,
        List<Slot> slots
) {

    public boolean supports(TimeWindow slot) {
        return slots.stream().anyMatch(candidate -> candidate.startTime().equals(slot.startTime())
                && candidate.endTime().equals(slot.endTime()));
    }

    public record Slot(LocalTime startTime, LocalTime endTime, boolean available) {
    }
}
