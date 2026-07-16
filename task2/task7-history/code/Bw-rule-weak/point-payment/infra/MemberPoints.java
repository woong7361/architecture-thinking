package com.thinking.ticket.infra;

import com.thinking.ticket.Points;
import com.thinking.ticket.provided.PointApi;

/** 사내 포인트 시스템 API를 포인트 역할에 맞춘다. */
public final class MemberPoints implements Points {

    private final PointApi api;

    public MemberPoints(PointApi api) {
        this.api = api;
    }

    @Override
    public boolean deduct(long userId, int amount) {
        return api.deduct(userId, amount);
    }
}
