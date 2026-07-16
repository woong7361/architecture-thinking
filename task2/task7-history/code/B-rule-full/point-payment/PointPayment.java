package com.thinking.ticket;

import com.thinking.ticket.provided.PointApi;

/** 회원의 포인트에서 받아낸다. 잔액이 모자라면 {@link InsufficientPointException}. */
final class PointPayment implements PaymentMethod {

    private final PointApi points;
    private final long userId;

    PointPayment(PointApi points, long userId) {
        this.points = points;
        this.userId = userId;
    }

    @Override
    public void pay(int amount) {
        if (!points.deduct(userId, amount)) {
            throw new InsufficientPointException(
                    "포인트 잔액이 모자란다. 회원: " + userId + ", 필요한 포인트: " + amount);
        }
    }
}
