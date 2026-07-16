package com.thinking.ticket.provided;

import java.util.HashMap;
import java.util.Map;

/**
 * 사내 포인트 시스템 API. 기존 인프라이며 수정할 수 없다.
 *
 * <p>실제 운영에서는 포인트 서버로 요청을 보내지만, 이 실험에서는 메모리로 동작하는 시뮬레이터다.
 * 그래서 테스트가 잔액을 심고({@link #seed}) 차감 기록을 되읽는 통로가 함께 열려 있다.
 *
 * <p>{@link #deduct}는 차감에 성공하면 true, 잔액이 모자라면 false를 반환한다.
 */
public final class PointApi {

    private final Map<Long, Integer> balances = new HashMap<>();
    private int deductCount;
    private int lastAmount;

    /** 테스트가 초기 잔액을 심는 통로. */
    public void seed(long userId, int balance) {
        balances.put(userId, balance);
    }

    public boolean deduct(long userId, int amount) {
        int balance = balances.getOrDefault(userId, 0);
        if (balance < amount) {
            return false;
        }
        balances.put(userId, balance - amount);
        deductCount++;
        lastAmount = amount;
        return true;
    }

    public int balanceOf(long userId) {
        return balances.getOrDefault(userId, 0);
    }

    /** 테스트가 호출 기록을 되읽는 통로. */
    public boolean wasDeducted() {
        return deductCount > 0;
    }

    public int deductCount() {
        return deductCount;
    }

    public int lastAmount() {
        return lastAmount;
    }
}
