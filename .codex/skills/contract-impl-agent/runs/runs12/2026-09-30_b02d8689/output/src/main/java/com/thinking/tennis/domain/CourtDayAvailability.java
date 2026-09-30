package com.thinking.tennis.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 코트 하나의 하루치 예약 상태다. 확인에 성공한 결과만 이 모양으로 남는다.
 *
 * <p>확인이 실패한 사실은 여기 담지 않는다. 실패는 돌려주는 데이터의 신선도로 드러나고, 신선도는 이
 * 결과를 읽는 쪽이 {@link #confirmedAt()} 과 견주어 판단한다.
 *
 * <p>코트의 이름과 예약 화면 주소도 여기 담지 않는다. 그 둘은 확인 시점의 사실이 아니라 지금 그 코트가
 * 무엇인지이고, 오래된 결과를 돌려줄 때 함께 낡으면 조회가 보여 준 링크와 알림에 실릴 링크가 갈린다.
 * 응답을 만드는 자리가 읽는 시점에 읽는다.
 *
 * @param confirmedAt 이 결과를 외부 예약처에서 가져오는 데 성공한 시각
 * @param slots       그 날짜에 이 코트가 운영하는 시간대 전부. 예약처가 날짜를 열지 않았으면 빈 목록이다
 */
public record CourtDayAvailability(String courtId,
                                   LocalDate date,
                                   Instant confirmedAt,
                                   List<SlotStatus> slots) {

    public CourtDayAvailability {
        Objects.requireNonNull(courtId, "courtId");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(confirmedAt, "confirmedAt");
        slots = List.copyOf(slots);
    }

    /**
     * 그 날짜의 운영 시간대를 이 결과로 판정할 수 있는지 여부다.
     *
     * <p>예약처가 아직 그 날짜를 열지 않았으면 확인은 성공하고 시간대가 하나도 오지 않는다. 그 빈 목록은
     * 이 코트가 그 날짜에 아무 시간대도 운영하지 않는다는 뜻이 아니라 아직 알 수 없다는 뜻이므로,
     * 판정할 수 없는 것으로 다룬다.
     */
    public boolean operatingSlotsKnown() {
        return !slots.isEmpty();
    }

    /** 이 코트가 그 날짜에 그 시간대를 운영하는지 여부다. 운영하지 않는 시간대로는 감시할 수 없다. */
    public boolean operates(TimeSlot slot) {
        return slots.stream().anyMatch(status -> status.slot().equals(slot));
    }

    /**
     * 그 시간대가 이 확인 시점에 비어 있었는지 여부다.
     *
     * <p>운영하지 않는 시간대는 비어 있지 않은 것으로 답한다. 없는 시간대의 빈자리를 묻는 것이므로
     * 참이 될 수 없고, 이 결과에 확인 실패는 담기지 않으므로 거짓이 실패를 뜻하지도 않는다.
     */
    public boolean isAvailable(TimeSlot slot) {
        return slots.stream().anyMatch(status -> status.slot().equals(slot) && status.available());
    }

    /** 그 날짜에 이 코트가 운영하는 시간대 전부다. 빈자리 여부는 담지 않는다. */
    public List<TimeSlot> operatingSlots() {
        return slots.stream().map(SlotStatus::slot).toList();
    }

    /**
     * 시간대 하나와 마지막 확인 시점의 빈자리 여부다.
     *
     * @param available {@link CourtDayAvailability#confirmedAt()} 시점에 비어 있었는지 여부. 확인이 실패했을 때의 값이 아니다
     */
    public record SlotStatus(TimeSlot slot, boolean available) {
    }
}
