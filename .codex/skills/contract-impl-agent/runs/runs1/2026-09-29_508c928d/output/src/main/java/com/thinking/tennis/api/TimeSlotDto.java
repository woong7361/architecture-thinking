package com.thinking.tennis.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = false)
public record TimeSlotDto(String startTime, String endTime) {
}
