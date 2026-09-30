package com.thinking.tennis.app;

import com.thinking.tennis.domain.TimeSlot;

import java.time.LocalDate;

public record CreateAlertCommand(String courtId, LocalDate date, TimeSlot slot) {
}
