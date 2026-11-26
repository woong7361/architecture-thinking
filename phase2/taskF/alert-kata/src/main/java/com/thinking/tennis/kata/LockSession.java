package com.thinking.tennis.kata;

// 창구가 작업에 넘기는 보유 집합이다. 작업 도중 알게 된 락을 이 자리로만 추가한다.
public interface LockSession {
    EscalateResult escalate(LockRequest request) throws InterruptedException;
}
