package com.thinking.tennis.app;

import com.thinking.tennis.domain.AvailabilitySnapshot;
import com.thinking.tennis.port.CourtAvailabilityPort;

public record AvailabilityResult(
        AvailabilitySnapshot snapshot,
        boolean stale,
        CourtAvailabilityPort.UpstreamFailureReason staleReason
) {
}
