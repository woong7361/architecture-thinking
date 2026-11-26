package com.thinking.tennis.kata;

import java.time.Duration;

// 대기 식과 두 상한을 함께 묶는다. budget이 1차 상한이고 maxAttempts는 안전망이다.
public record RetryPolicy(RetryBackoff backoff, Duration base, Duration cap, Duration budget, int maxAttempts) {
    public RetryPolicy {
        if (base.isNegative() || cap.isNegative() || budget.isNegative()) {
            throw new IllegalArgumentException("Durations must not be negative");
        }
        if (cap.compareTo(base) < 0) {
            throw new IllegalArgumentException("cap must not be shorter than base");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
    }

    // 대기 없이 곧바로 다시 시도한다. 백오프를 넣기 전의 동작이다.
    public static RetryPolicy immediate(int maxAttempts, Duration budget) {
        return new RetryPolicy(RetryBackoff.NONE, Duration.ZERO, Duration.ZERO, budget, maxAttempts);
    }

    Duration waitBefore(int attemptNumber) {
        return backoff.waitBefore(attemptNumber, base, cap);
    }
}
