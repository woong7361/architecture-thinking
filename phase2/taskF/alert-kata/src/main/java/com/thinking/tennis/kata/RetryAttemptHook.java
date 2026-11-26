package com.thinking.tennis.kata;

// 시도가 저장소에 닿는 시각을 밖에서 받는다. 첫 시도와 재시도를 가를 수 있게 차수를 함께 넘긴다.
public interface RetryAttemptHook {
    RetryAttemptHook NONE = new RetryAttemptHook() {
    };

    default void attempted(int attemptNumber, long nanoTime) {
    }
}
