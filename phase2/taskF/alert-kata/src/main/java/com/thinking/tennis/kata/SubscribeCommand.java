package com.thinking.tennis.kata;

import java.time.LocalDate;

public record SubscribeCommand(long userId, long courtId, LocalDate playDate, String timeSlot) {
}
