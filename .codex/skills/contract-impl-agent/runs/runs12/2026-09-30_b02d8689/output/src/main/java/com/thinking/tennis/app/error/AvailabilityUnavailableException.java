package com.thinking.tennis.app.error;

import com.thinking.tennis.domain.UpstreamFailureReason;

/**
 * 외부 예약처 확인이 실패했고 저장된 마지막 성공 결과도 없다.
 *
 * <p>저장된 성공 결과가 있으면 이 예외가 아니라 오래된 결과를 돌려준다. 저장된 성공 결과가 한 번도
 * 없을 때만 실패로 알린다.
 */
public class AvailabilityUnavailableException extends RuntimeException {

    private final UpstreamFailureReason reason;

    public AvailabilityUnavailableException(UpstreamFailureReason reason) {
        super("예약 상태를 확인하지 못했습니다: " + reason);
        this.reason = reason;
    }

    public UpstreamFailureReason reason() {
        return reason;
    }
}
