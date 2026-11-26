package com.thinking.tennis.kata;

import java.time.Duration;

// 재시도 한 판의 집계. conflicts는 재시도로 이어진 충돌과 최종 실패를 모두 센다.
public record RetryMetrics(long attempts, long conflicts, long exhausted, Duration totalWait) {
    public double conflictRate() {
        return attempts == 0 ? 0.0 : (double) conflicts / attempts;
    }
}
