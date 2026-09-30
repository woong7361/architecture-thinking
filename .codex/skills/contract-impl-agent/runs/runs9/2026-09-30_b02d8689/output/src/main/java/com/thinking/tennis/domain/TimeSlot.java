package com.thinking.tennis.domain;

import java.time.LocalTime;

public record TimeSlot(LocalTime startTime, LocalTime endTime) {
}
