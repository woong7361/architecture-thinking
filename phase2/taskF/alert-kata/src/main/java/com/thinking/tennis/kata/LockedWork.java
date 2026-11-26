package com.thinking.tennis.kata;

// 락을 쥔 구간에서 수행하는 작업이다.
public interface LockedWork {
    void run(LockSession session) throws InterruptedException;
}
