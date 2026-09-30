package com.thinking.tennis.app;

import com.thinking.tennis.port.CourtAvailabilityPort;

public final class AvailabilityFailureException extends AlertApplicationException {

    private final CourtAvailabilityPort.UpstreamFailureReason reason;
    private final boolean hasPreviousResult;

    public AvailabilityFailureException(CourtAvailabilityPort.UpstreamFailureReason reason,
                                        boolean hasPreviousResult) {
        super("외부 예약처 확인 실패: " + reason);
        this.reason = reason;
        this.hasPreviousResult = hasPreviousResult;
    }

    public CourtAvailabilityPort.UpstreamFailureReason reason() {
        return reason;
    }

    public boolean hasPreviousResult() {
        return hasPreviousResult;
    }
}
