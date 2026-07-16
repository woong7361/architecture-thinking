package com.thinking.ticket;

import com.thinking.ticket.provided.PointApi;

/** 포인트로 값을 받는다. 카드정보는 쓰지 않는다. */
final class PointPayment implements Payment {

    private final PointApi points;

    PointPayment(PointApi points) {
        this.points = points;
    }

    @Override
    public void pay(long userId, int amount) {
        if (!points.deduct(userId, amount)) {
            throw new InsufficientPointException("포인트 잔액이 부족합니다: " + amount + "점이 필요합니다");
        }
    }
}
