package com.thinking.tennis.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = false)
public record AlertRequestDto(String courtId, String date, TimeSlotDto slot) {
}
