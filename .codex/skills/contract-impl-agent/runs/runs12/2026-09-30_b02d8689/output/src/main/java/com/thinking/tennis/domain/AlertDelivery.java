package com.thinking.tennis.domain;

import java.time.Instant;

/**
 * 신청 하나에 대한 알림 발송 결과다. 신청 하나에 알림은 한 건만 보낸다.
 *
 * <p>실제로 보내기 전까지는 취소될 수 있어서 {@code PENDING} 이던 대기가 사라지고 발송 결과가 다시
 * 없는 상태로 돌아가는 경우가 있다. 최신 확인 결과에 빈자리가 없어 보내지 않기로 했을 때와 신청이
 * 해제되거나 만료됐을 때다.
 *
 * @param attemptCount  발송을 시도한 횟수. 아직 보내지 않았으면 0이고 상한은 최초 발송 한 번에 재시도 세 번을 더한 값이다
 * @param lastAttemptAt 마지막으로 시도한 시각. 아직 보내지 않았으면 null이다
 * @param failureReason 실패한 경우 사람이 읽을 사유. 실패하지 않았으면 null이다
 */
public record AlertDelivery(DeliveryStatus status,
                            int attemptCount,
                            Instant lastAttemptAt,
                            String failureReason) {

    /** 발송을 시도한 횟수의 상한이다. 최초 발송 한 번에 재시도 세 번을 더한 값이다. */
    public static final int MAX_ATTEMPTS = 4;

    /** 발송 대기에 올랐고 아직 보내지 않은 상태다. */
    public static AlertDelivery pending() {
        return new AlertDelivery(DeliveryStatus.PENDING, 0, null, null);
    }

    /** 아직 보내지 않았는지 여부다. 이 대기는 해제·만료로 버려질 수 있다. */
    public boolean isPending() {
        return status == DeliveryStatus.PENDING;
    }
}
