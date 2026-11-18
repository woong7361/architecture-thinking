package com.thinking.tennis.kata;

import java.time.Duration;
import java.util.Optional;

public final class DistributedAlertService {
    private final AlertService inner;
    private final RedisSpinLock lock;
    private final Duration acquireWait;
    private final Duration leaseGuard;
    private final LockedSectionHook hook;

    public DistributedAlertService(AlertService inner, RedisSpinLock lock, Duration acquireWait,
                                   Duration leaseGuard, LockedSectionHook hook) {
        this.inner = inner;
        this.lock = lock;
        this.acquireWait = acquireWait;
        this.leaseGuard = leaseGuard;
        this.hook = hook;
    }

    public SubscribeOutcome subscribe(SubscribeCommand command) throws InterruptedException {
        String key = AlertLockKey.of(command);
        long waitStarted = System.nanoTime();
        Optional<LockToken> acquired = acquireWait.isZero() ? lock.tryAcquire(key) : lock.acquire(key, acquireWait);
        double waitMillis = millisSince(waitStarted);
        if (acquired.isEmpty()) {
            return new SubscribeOutcome(SubscribeOutcome.Status.NOT_ACQUIRED, null, false, null, waitMillis, 0);
        }

        LockToken token = acquired.get();
        if (!leaseGuard.isZero() && lock.remainingLease(token).compareTo(leaseGuard) < 0) {
            return new SubscribeOutcome(SubscribeOutcome.Status.LEASE_TOO_SHORT, null, true,
                    lock.release(token), waitMillis, 0);
        }

        long workStarted = System.nanoTime();
        boolean completed = false;
        try {
            hook.entered(token);
            long subscriptionId = inner.subscribe(command);
            double workMillis = millisSince(workStarted);
            completed = true;
            boolean ownedAtExit = lock.heldBy(token);
            hook.beforeExit(token, ownedAtExit);
            ReleaseResult released = hook.skipRelease() ? null : lock.release(token);
            return new SubscribeOutcome(SubscribeOutcome.Status.COMPLETED, subscriptionId, ownedAtExit,
                    released, waitMillis, workMillis);
        } finally {
            if (!completed && !hook.skipRelease()) {
                lock.release(token);
            }
        }
    }

    private double millisSince(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000.0;
    }
}
