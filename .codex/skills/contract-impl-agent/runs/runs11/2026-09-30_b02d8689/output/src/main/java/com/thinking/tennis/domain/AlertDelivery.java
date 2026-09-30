package com.thinking.tennis.domain;

import java.time.Instant;

/**
 * 신청 하나에 대한 알림 발송 결과다. 신청 하나에 알림은 한 건만 보낸다.
 *
 * @param status        발송 결과
 * @param attemptCount  시도한 횟수. 아직 보내지 않았으면 0이고 상한은 최초 한 번에 재시도 세 번을 더한 값이다
 * @param lastAttemptAt 마지막으로 시도한 시각. 아직 보내지 않았으면 null이다
 * @param failureReason 실패한 경우 사람이 읽을 사유. 실패하지 않았으면 null이다
 */
public record AlertDelivery(DeliveryStatus status,
                            int attemptCount,
                            Instant lastAttemptAt,
                            String failureReason) {

    /** 실제로 보내기 전까지는 취소될 수 있는 대기인지 여부다. */
    public boolean pending() {
        return status == DeliveryStatus.PENDING;
    }
}
