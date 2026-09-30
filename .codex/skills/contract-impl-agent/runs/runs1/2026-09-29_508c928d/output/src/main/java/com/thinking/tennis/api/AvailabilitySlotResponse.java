package com.thinking.tennis.api;

public record AvailabilitySlotResponse(String startTime, String endTime, boolean available) {
}
