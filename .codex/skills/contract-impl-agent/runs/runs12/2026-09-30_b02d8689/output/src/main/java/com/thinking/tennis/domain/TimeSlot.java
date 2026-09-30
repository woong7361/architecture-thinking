package com.thinking.tennis.domain;

import java.time.LocalTime;
import java.util.Objects;

/**
 * 예약 단위가 되는 시간대다. 코트가 운영하는 시간대와 정확히 일치해야 한다.
 *
 * <p>시각은 코트가 있는 지역 기준이다. 지역의 시간대는 {@code Asia/Seoul} 이고, 그 값은 유스케이스가
 * 만료 시각을 계산할 때 쓴다.
 *
 * @param startTime 시작 시각
 * @param endTime   종료 시각
 */
public record TimeSlot(LocalTime startTime, LocalTime endTime) {

    public TimeSlot {
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(endTime, "endTime");
    }
}
