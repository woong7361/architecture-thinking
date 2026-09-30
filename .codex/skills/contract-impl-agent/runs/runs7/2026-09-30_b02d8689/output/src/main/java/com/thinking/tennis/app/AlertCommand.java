package com.thinking.tennis.app;

import java.time.LocalDate;
import java.time.LocalTime;

public record AlertCommand(
        String courtId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
) {
    public String fingerprint() {
        return courtId + "|" + date + "|" + startTime + "|" + endTime;
    }
}
