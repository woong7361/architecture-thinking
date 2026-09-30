package com.thinking.tennis.domain;

import java.time.LocalTime;

public record TimeWindow(LocalTime startTime, LocalTime endTime) {
}
