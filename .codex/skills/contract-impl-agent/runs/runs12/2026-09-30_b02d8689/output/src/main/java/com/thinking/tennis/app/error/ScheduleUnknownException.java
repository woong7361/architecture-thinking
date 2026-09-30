package com.thinking.tennis.app.error;

import com.thinking.tennis.domain.UpstreamFailureReason;

import java.time.LocalDate;

/**
 * 그 코트·날짜의 운영 시간대를 알 수 없어 신청 조건을 판정할 수 없다.
 *
 * <p>확인이 실패했고 저장된 성공 결과도 없을 때 일어난다. 알림 신청은 그 코트가 그 날짜에 운영하는
 * 시간대를 알아야 조건을 판정할 수 있고, 모르는 상태에서 지원하지 않는 시간대로 거절하면 형식이 맞는
 * 요청을 서버 사정으로 되돌릴 수 없게 거절하는 것이 된다.
 *
 * <p>무엇이 실패했는지를 함께 지닌다. 계약은 이 실패를 예약 상태 조회와 같은 조건으로 알리라고 하고,
 * 실패한 이유마다 상태 코드와 에러 코드가 다르다. 이유를 버리면 외부 장애가 서버 결함으로 보고된다.
 */
public class ScheduleUnknownException extends RuntimeException {

    private final UpstreamFailureReason reason;

    public ScheduleUnknownException(String courtId, LocalDate date, UpstreamFailureReason reason) {
        super("운영 시간대를 확인하지 못했습니다: " + courtId + " " + date + " (" + reason + ")");
        this.reason = reason;
    }

    public UpstreamFailureReason reason() {
        return reason;
    }
}
