package com.thinking.tennis.api;

import com.thinking.tennis.port.CourtAvailabilityPort.UpstreamFailureReason;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(
        String courtId,
        String courtName,
        LocalDate date,
        Instant confirmedAt,
        boolean stale,
        UpstreamFailureReason staleReason,
        String reservationUrl,
        List<AvailabilitySlotResponse> slots
) {
}
