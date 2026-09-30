package com.thinking.tennis.domain;

import com.thinking.tennis.port.CourtAvailabilityPort;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AvailabilitySnapshot(
        CourtAvailabilityPort.SupportedCourt court,
        LocalDate date,
        Instant confirmedAt,
        boolean stale,
        CourtAvailabilityPort.UpstreamFailureReason staleReason,
        List<CourtAvailabilityPort.SlotAvailability> slots
) {
    public AvailabilitySnapshot {
        slots = List.copyOf(slots);
    }
}
