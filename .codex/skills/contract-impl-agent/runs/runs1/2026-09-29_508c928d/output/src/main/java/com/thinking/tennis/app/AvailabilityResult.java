package com.thinking.tennis.app;

import com.thinking.tennis.domain.Availability;
import com.thinking.tennis.port.CourtAvailabilityPort.UpstreamFailureReason;

public record AvailabilityResult(
        Availability availability,
        boolean stale,
        UpstreamFailureReason staleReason
) {
}
