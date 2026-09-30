package com.thinking.tennis.app;

import com.thinking.tennis.domain.TimeSlot;

import java.time.LocalDate;

/**
 * 알림 신청 조건이다. 코트 하나, 날짜 하나, 시간대 하나다.
 *
 * <p>같은 조건을 다시 신청했는지, 멱등 키가 다른 내용에 쓰였는지를 이 값으로 판정한다.
 */
public record AlertCondition(String courtId, LocalDate date, TimeSlot slot) {

    /**
     * 멱등 키가 같은 내용의 요청에 쓰였는지 견주는 지문이다.
     *
     * <p>계약은 같은 키가 다른 내용의 요청에 쓰이면 거절하라고 하지만 무엇이 같은 내용인지는 정하지
     * 않았다. 요청 본문이 조건 셋뿐이므로 그 셋을 정규화한 문자열을 지문으로 쓴다.
     */
    public String fingerprint() {
        return courtId + "|" + date + "|" + slot.startTime() + "-" + slot.endTime();
    }
}
