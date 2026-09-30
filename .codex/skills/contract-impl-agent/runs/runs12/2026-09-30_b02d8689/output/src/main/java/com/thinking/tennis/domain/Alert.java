package com.thinking.tennis.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 알림 신청 하나다. 코트 하나, 날짜 하나, 시간대 하나가 신청 단위다.
 *
 * <p>상태 전이는 한 방향이다. 감시 중으로 시작해 알림 완료·해제·만료 중 하나로 끝나고, 끝난 신청은
 * 되살아나지 않는다. 그래서 상태를 바꾸는 메서드는 이미 끝난 신청을 그대로 돌려준다.
 *
 * <p>마지막 확인 시각과 확인 지연 여부는 이 신청의 것이 아니라 이 신청이 보는 코트·날짜의 확인 결과에서
 * 나오므로 여기 담지 않는다. 읽는 시점에 확인 결과에서 계산한다.
 *
 * @param expiresAt 만료 시각. 이용 시작 시각이다
 */
public record Alert(String alertId,
                    String userId,
                    String courtId,
                    String courtName,
                    String reservationUrl,
                    LocalDate date,
                    TimeSlot slot,
                    AlertStatus status,
                    Instant createdAt,
                    Instant expiresAt,
                    AlertDelivery delivery) {

    public Alert {
        Objects.requireNonNull(alertId, "alertId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(courtId, "courtId");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    /** 감시 중으로 시작하는 새 신청이다. 발송 대기에 오른 적이 없으므로 발송 결과는 없다. */
    public static Alert watching(String alertId,
                                 String userId,
                                 String courtId,
                                 String courtName,
                                 String reservationUrl,
                                 LocalDate date,
                                 TimeSlot slot,
                                 Instant createdAt,
                                 Instant expiresAt) {
        return new Alert(alertId, userId, courtId, courtName, reservationUrl, date, slot,
                AlertStatus.WATCHING, createdAt, expiresAt, null);
    }

    public boolean ownedBy(String candidateUserId) {
        return userId.equals(candidateUserId);
    }

    public boolean watches(String candidateCourtId, LocalDate candidateDate, TimeSlot candidateSlot) {
        return status == AlertStatus.WATCHING
                && courtId.equals(candidateCourtId)
                && date.equals(candidateDate)
                && slot.equals(candidateSlot);
    }

    /**
     * 해제한다. 아직 보내지 않은 발송 대기는 버린다.
     *
     * <p>이미 끝난 신청은 상태를 바꾸지 않고 그대로 돌려준다. 해제를 여러 번 불러도 결과가 같아야 한다.
     */
    public Alert canceled() {
        if (status.isTerminal()) {
            return this;
        }
        return withStatus(AlertStatus.CANCELED);
    }

    /**
     * 만료시킨다. 아직 보내지 않은 발송 대기는 버린다.
     */
    public Alert expired() {
        if (status.isTerminal()) {
            return this;
        }
        return withStatus(AlertStatus.EXPIRED);
    }

    /** 만료 시각이 지났는지 여부다. 만료로 옮기는 일은 서버 작업이 하므로 판정과 전이를 분리한다. */
    public boolean isDue(Instant now) {
        return !expiresAt.isAfter(now);
    }

    /**
     * 빈자리를 확인해 발송 대기에 올린다.
     *
     * <p>신청 하나에 알림은 한 건만 보내므로 이미 발송 결과가 있는 신청은 그대로 돌려준다. 같은 자리를
     * 반복해서 발견해도 대기가 겹치지 않는다. 감시 중이 아닌 신청은 확인과 발송을 멈춘 것이라 오르지 않는다.
     *
     * <p>보내는 일은 이 신청의 몫이 아니다. 대기에 올랐다는 사실까지만 여기 남고, 실제로 보내는 것과
     * 알림 완료로 옮기는 것은 발송 작업이 한다.
     */
    public Alert deliveryPending() {
        if (status != AlertStatus.WATCHING || delivery != null) {
            return this;
        }
        return withDelivery(AlertDelivery.pending());
    }

    /**
     * 아직 보내지 않은 발송 대기를 버린다. 신청은 감시 중으로 남는다.
     *
     * <p>최신 확인 결과에 빈자리가 없으면 이전 결과로 만든 대기를 보내지 않는다. 이미 나간 알림은
     * 취소되지 않으므로 {@code PENDING} 이 아닌 결과는 그대로 남긴다.
     */
    public Alert pendingDeliveryDiscarded() {
        if (delivery == null || !delivery.isPending()) {
            return this;
        }
        return withDelivery(null);
    }

    private Alert withStatus(AlertStatus next) {
        return new Alert(alertId, userId, courtId, courtName, reservationUrl, date, slot,
                next, createdAt, expiresAt, discardPendingDelivery());
    }

    private Alert withDelivery(AlertDelivery next) {
        return new Alert(alertId, userId, courtId, courtName, reservationUrl, date, slot,
                status, createdAt, expiresAt, next);
    }

    /*
     * 감시와 발송을 멈출 때 아직 보내지 않은 대기는 버린다. 이미 나간 알림은 취소되지 않으므로
     * PENDING 이 아닌 결과는 그대로 남긴다.
     */
    private AlertDelivery discardPendingDelivery() {
        return delivery != null && delivery.isPending() ? null : delivery;
    }
}
