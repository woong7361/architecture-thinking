package com.thinking.ticket;

import com.thinking.ticket.provided.PointApi;

/**
 * 포인트로 청구하는 결제 담당.
 *
 * <p>{@link ChargePort}의 두 번째 구현이다. 카드 쪽은 한 줄도 열지 않고 여기 한 장이 추가됐다(OCP).
 *
 * <p>잔액 확인을 먼저 묻고 차감하지 않는다 — {@link PointApi#deduct}가 "모자라면 차감하지 않고 false"를
 * 이미 원자적으로 보장하므로, 되물으면 확인과 차감 사이가 벌어질 뿐이다(Tell, Don't Ask).
 */
final class PointPaymentAdapter implements ChargePort {

    private final PointApi points;
    private final long userId;

    PointPaymentAdapter(PointApi points, long userId) {
        this.points = points;
        this.userId = userId;
    }

    @Override
    public void charge(int amount) {
        if (!points.deduct(userId, amount)) {
            throw new InsufficientPointException(userId, amount);
        }
    }
}
