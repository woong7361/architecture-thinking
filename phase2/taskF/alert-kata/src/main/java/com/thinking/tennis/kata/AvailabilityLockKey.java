package com.thinking.tennis.kata;

import java.time.LocalDate;

public final class AvailabilityLockKey {
    private AvailabilityLockKey() {
    }

    public static String of(long courtId, LocalDate playDate) {
        return "lock:availability:court" + courtId + ":" + playDate;
    }
}
