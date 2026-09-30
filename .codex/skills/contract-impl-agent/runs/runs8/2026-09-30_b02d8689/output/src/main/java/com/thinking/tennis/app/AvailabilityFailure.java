package com.thinking.tennis.app;

import com.thinking.tennis.port.CourtAvailabilityPort;

public final class AvailabilityFailure extends RuntimeException {
    private final CourtAvailabilityPort.UpstreamFailureReason reason;

    public AvailabilityFailure(CourtAvailabilityPort.UpstreamFailureReason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public CourtAvailabilityPort.UpstreamFailureReason reason() {
        return reason;
    }
}
