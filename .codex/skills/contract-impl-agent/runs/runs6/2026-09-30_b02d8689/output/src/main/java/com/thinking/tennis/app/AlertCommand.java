package com.thinking.tennis.app;

public record AlertCommand(String courtId,
                           String date,
                           TimeSlotCommand slot,
                           boolean hasExtraProperties) {

    public record TimeSlotCommand(String startTime,
                                  String endTime,
                                  boolean hasExtraProperties) {
    }
}
