package com.thinking.tennis.kata;

public final class AlertLockKey {
    private AlertLockKey() {
    }

    public static String of(SubscribeCommand command) {
        return "lock:alert:court" + command.courtId() + ":" + command.playDate() + ":" + command.timeSlot();
    }
}
