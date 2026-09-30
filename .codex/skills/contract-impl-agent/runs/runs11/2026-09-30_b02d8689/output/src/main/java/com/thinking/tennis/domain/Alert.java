package com.thinking.tennis.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 알림 신청 하나다.
 *
 * <p>여기에는 저장된 사실만 둔다. 마지막 확인 시각과 확인 지연 여부는 이 신청의 코트·날짜를 확인한 기록에서
 * 나오는 값이라 신청에 적어 두지 않고 조회할 때 붙인다. 신청마다 베껴 두면 확인 한 번이 여러 신청을 고쳐야
 * 하고, 고치지 못한 신청이 남으면 같은 코트·날짜의 두 신청이 다른 확인 시각을 말한다.
 *
 * @param sequence 신청이 만들어진 순서. 목록을 최근 것부터 내보내려면 같은 시각에 만들어진 두 신청 사이에도
 *                 순서가 있어야 한다
 */
public record Alert(UUID alertId,
                    String userId,
                    Court court,
                    LocalDate date,
                    TimeSlot slot,
                    AlertStatus status,
                    Instant createdAt,
                    Instant expiresAt,
                    AlertDelivery delivery,
                    long sequence) {

    public boolean watching() {
        return status == AlertStatus.WATCHING;
    }

    /** 신청 조건이 같은지 본다. 사용자는 이 비교 밖에서 가른다. */
    public boolean sameCondition(String otherCourtId, LocalDate otherDate, TimeSlot otherSlot) {
        return court.courtId().equals(otherCourtId)
                && date.equals(otherDate)
                && slot.equals(otherSlot);
    }

    /**
     * 사용자가 해제한 신청이다. 이미 끝난 신청은 그대로 둔다. 여러 번 불러도 결과가 같아야 하고,
     * 끝난 신청을 다시 옮기면 되살아나지 않는다는 약속이 깨진다.
     */
    public Alert canceled() {
        return watching() ? withStatus(AlertStatus.CANCELED) : this;
    }

    /** 만료 시각이 지나 확인과 발송을 멈춘 신청이다. 이미 끝난 신청은 그대로 둔다. */
    public Alert expired() {
        return watching() ? withStatus(AlertStatus.EXPIRED) : this;
    }

    private Alert withStatus(AlertStatus next) {
        return new Alert(alertId, userId, court, date, slot, next, createdAt, expiresAt,
                dropPendingDelivery(), sequence);
    }

    /**
     * 아직 보내지 않은 발송 대기는 버린다. 이미 나간 알림은 취소되지 않으므로 {@code SENT} 와
     * {@code FAILED} 는 그대로 남긴다.
     */
    private AlertDelivery dropPendingDelivery() {
        return delivery != null && delivery.pending() ? null : delivery;
    }
}
