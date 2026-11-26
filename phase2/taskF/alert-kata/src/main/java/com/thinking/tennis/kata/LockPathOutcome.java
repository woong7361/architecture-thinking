package com.thinking.tennis.kata;

// 락 두 개를 차례로 쥐는 경로 한 건의 결과다. 대기를 획득 순서대로 담아 멈춤이 어느 자리에서 생겼는지 가린다.
public record LockPathOutcome(Status status, double firstWaitMillis, double secondWaitMillis,
                              boolean ownedFirstLockAtSecondAcquire) {
    public enum Status {
        COMPLETED,
        FIRST_NOT_ACQUIRED,
        SECOND_NOT_ACQUIRED
    }
}
