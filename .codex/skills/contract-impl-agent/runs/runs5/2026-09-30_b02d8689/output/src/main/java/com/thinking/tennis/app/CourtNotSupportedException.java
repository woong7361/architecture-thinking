package com.thinking.tennis.app;

public final class CourtNotSupportedException extends AlertApplicationException {

    private final String courtId;

    public CourtNotSupportedException(String courtId) {
        super("지원하지 않는 코트: " + courtId);
        this.courtId = courtId;
    }

    public String courtId() {
        return courtId;
    }
}
