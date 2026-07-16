package com.thinking.ticket;

/**
 * 포인트로 값을 치르는 자.
 *
 * <p>잔액이 모자라는지는 묻지 않는다. 잔액을 가진 쪽이 그 규칙을 스스로 지키므로,
 * 차감을 시도하고 그쪽이 거절했을 때 그것을 포인트 부족으로 읽는다. 물어본 뒤 검사하면
 * 같은 규칙이 두 곳에 살게 된다.
 */
final class PointPayer implements Payer {

    private final Points points;
    private final long userId;

    PointPayer(Points points, long userId) {
        this.points = points;
        this.userId = userId;
    }

    @Override
    public void pay(int amount) {
        if (!points.deduct(userId, amount)) {
            throw new InsufficientPointException(userId, amount);
        }
    }
}
