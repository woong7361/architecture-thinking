package com.thinking.tennis.kata;

// 경로가 락을 쥔 구간에서 수행하는 작업이다. 저장소 연동을 넣기 전까지는 측정과 시험이 이 자리를 채운다.
public interface LockPathSteps {
    LockPathSteps NONE = new LockPathSteps() {
    };

    default void afterFirstLocked() throws InterruptedException {
    }

    default void afterBothLocked() throws InterruptedException {
    }
}
