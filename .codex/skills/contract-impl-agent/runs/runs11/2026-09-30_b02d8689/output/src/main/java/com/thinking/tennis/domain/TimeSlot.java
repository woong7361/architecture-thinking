package com.thinking.tennis.domain;

import java.time.LocalTime;

/**
 * 예약 단위가 되는 시간대다. 코트가 운영하는 시간대와 정확히 일치해야 한다.
 *
 * <p>시각은 코트가 있는 지역 기준의 벽시계 시각이라 {@link LocalTime} 으로 든다. 지역과 날짜를 붙여
 * 시점으로 만드는 일은 만료 시각을 계산하는 유스케이스가 한다.
 */
public record TimeSlot(LocalTime startTime, LocalTime endTime) {

    public TimeSlot {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("시간대는 시작 시각과 종료 시각을 모두 가진다");
        }
    }
}
