package com.thinking.tennis.app.error;

import com.thinking.tennis.domain.TimeSlot;

import java.time.LocalDate;
import java.util.List;

/**
 * 코트는 지원하지만 그 코트가 그 날짜에 운영하지 않는 시간대다.
 *
 * <p>예약처가 아직 그 날짜를 열지 않아 운영 시간대가 하나도 없는 경우도 여기에 든다. 그 날짜에 운영하는
 * 시간대 목록에 없는 시간대라는 사실이 같다.
 *
 * <p>그 날짜에 운영하는 시간대 목록을 함께 지닌다. 사용자가 시간대를 고쳐 다시 신청하려면 어떤 시간대가
 * 되는지를 알아야 하고, 안 되는 것만 알려 주면 예약 상태 조회를 한 번 더 부르게 된다. 선착순 예약에서
 * 그 왕복은 비싸다. 예약처가 그 날짜를 열지 않았으면 그 목록은 비어 있다.
 */
public class SlotNotSupportedException extends RuntimeException {

    private final String courtId;
    private final LocalDate date;
    private final TimeSlot slot;
    private final List<TimeSlot> operatingSlots;

    public SlotNotSupportedException(String courtId,
                                     LocalDate date,
                                     TimeSlot slot,
                                     List<TimeSlot> operatingSlots) {
        super("지원하지 않는 시간대: " + courtId + " " + date + " " + slot.startTime() + "~" + slot.endTime());
        this.courtId = courtId;
        this.date = date;
        this.slot = slot;
        this.operatingSlots = List.copyOf(operatingSlots);
    }

    public String courtId() {
        return courtId;
    }

    public LocalDate date() {
        return date;
    }

    public TimeSlot slot() {
        return slot;
    }

    public List<TimeSlot> operatingSlots() {
        return operatingSlots;
    }
}
