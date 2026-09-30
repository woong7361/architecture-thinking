package com.thinking.tennis.api.dto;

import com.thinking.tennis.domain.TimeSlot;

import java.time.format.DateTimeFormatter;

/**
 * 도메인 값을 계약이 정한 표현으로 옮긴다.
 *
 * <p>시각은 24시간제 {@code HH:mm} 이고 코트가 있는 지역 기준이다. 타임스탬프는 UTC로 표기한다.
 */
final class Payloads {

    private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");

    static TimeSlotPayload slot(TimeSlot slot) {
        return new TimeSlotPayload(HOUR_MINUTE.format(slot.startTime()), HOUR_MINUTE.format(slot.endTime()));
    }

    static String time(java.time.LocalTime value) {
        return HOUR_MINUTE.format(value);
    }

    private Payloads() {
    }
}
