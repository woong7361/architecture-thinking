package com.thinking.tennis.kata;

import java.time.Duration;
import java.util.Optional;

// 두 경로가 공유하는 획득 절차다. 순서는 호출한 경로가 넘긴 대로 따르고 여기서 바꾸지 않는다.
final class LockPaths {
    private LockPaths() {
    }

    static LockPathOutcome acquireBoth(RedisSpinLock lock, Duration acquireWait, LockPathSteps steps,
                                       String firstKey, String secondKey) throws InterruptedException {
        long firstStarted = System.nanoTime();
        Optional<LockToken> firstToken = lock.acquire(firstKey, acquireWait);
        double firstWait = millisSince(firstStarted);
        if (firstToken.isEmpty()) {
            return new LockPathOutcome(LockPathOutcome.Status.FIRST_NOT_ACQUIRED, firstWait, 0, false);
        }

        try {
            steps.afterFirstLocked();

            long secondStarted = System.nanoTime();
            Optional<LockToken> secondToken = lock.acquire(secondKey, acquireWait);
            double secondWait = millisSince(secondStarted);
            boolean ownedFirst = lock.heldBy(firstToken.get());
            if (secondToken.isEmpty()) {
                return new LockPathOutcome(LockPathOutcome.Status.SECOND_NOT_ACQUIRED, firstWait, secondWait,
                        ownedFirst);
            }

            try {
                steps.afterBothLocked();
                return new LockPathOutcome(LockPathOutcome.Status.COMPLETED, firstWait, secondWait, ownedFirst);
            } finally {
                lock.release(secondToken.get());
            }
        } finally {
            lock.release(firstToken.get());
        }
    }

    private static double millisSince(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000.0;
    }
}
