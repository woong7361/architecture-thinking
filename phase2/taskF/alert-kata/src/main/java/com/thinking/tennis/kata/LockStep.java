package com.thinking.tennis.kata;

// 락 한 건의 획득 기록이다. 앞서 쥔 락을 그 시점에도 보유했는지 함께 담아 기한 만료를 가린다.
public record LockStep(LockRequest request, double waitMillis, boolean acquired,
                       boolean allPreviousHeldAtAcquire, ReleaseResult releaseResult) {
}
