package com.thinking.tennis.kata;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OptimisticAlertService {
    private static final int DEFAULT_MAX_ATTEMPTS = 5;
    private static final Duration DEFAULT_BUDGET = Duration.ofSeconds(5);

    private final OptimisticSubscriptionAttempt attempt;
    private final RetryPolicy policy;
    private final RetryAttemptHook hook;
    private final AtomicLong attemptCount = new AtomicLong();
    private final AtomicLong conflictCount = new AtomicLong();
    private final AtomicLong exhaustedCount = new AtomicLong();
    private final AtomicLong waitNanos = new AtomicLong();

    @Autowired
    public OptimisticAlertService(OptimisticSubscriptionAttempt attempt) {
        this(attempt, RetryPolicy.immediate(DEFAULT_MAX_ATTEMPTS, DEFAULT_BUDGET));
    }

    public OptimisticAlertService(OptimisticSubscriptionAttempt attempt, RetryPolicy policy) {
        this(attempt, policy, RetryAttemptHook.NONE);
    }

    public OptimisticAlertService(OptimisticSubscriptionAttempt attempt, RetryPolicy policy,
                                  RetryAttemptHook hook) {
        this.attempt = attempt;
        this.policy = policy;
        this.hook = hook;
    }

    public long subscribe(SubscribeCommand command) {
        long deadlineNanos = System.nanoTime() + policy.budget().toNanos();
        for (int attemptNumber = 1; attemptNumber <= policy.maxAttempts(); attemptNumber++) {
            attemptCount.incrementAndGet();
            hook.attempted(attemptNumber, System.nanoTime());
            try {
                return attempt.subscribe(command);
            } catch (OptimisticConflictException conflict) {
                conflictCount.incrementAndGet();
                if (attemptNumber == policy.maxAttempts()) {
                    exhaustedCount.incrementAndGet();
                    throw conflict;
                }
                Duration wait = policy.waitBefore(attemptNumber);
                // 기다린 뒤에 예산이 모자라 실패하면 그 대기는 모두 버려지므로 먼저 비교한다.
                if (wait.toNanos() > deadlineNanos - System.nanoTime()) {
                    exhaustedCount.incrementAndGet();
                    throw conflict;
                }
                if (!sleep(wait)) {
                    exhaustedCount.incrementAndGet();
                    throw conflict;
                }
            }
        }
        throw new IllegalStateException("Unreachable retry state");
    }

    public RetryPolicy policy() {
        return policy;
    }

    public RetryMetrics metrics() {
        return new RetryMetrics(attemptCount.get(), conflictCount.get(), exhaustedCount.get(),
                Duration.ofNanos(waitNanos.get()));
    }

    public long conflictCount() {
        return conflictCount.get();
    }

    public void resetMetrics() {
        attemptCount.set(0);
        conflictCount.set(0);
        exhaustedCount.set(0);
        waitNanos.set(0);
    }

    // 중단 요청을 받으면 더 기다리지 않고 플래그를 되살린 뒤 false를 돌려준다.
    private boolean sleep(Duration wait) {
        if (wait.isZero() || wait.isNegative()) {
            return true;
        }
        long started = System.nanoTime();
        try {
            Thread.sleep(wait.toMillis(), wait.toNanosPart() % 1_000_000);
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            waitNanos.addAndGet(System.nanoTime() - started);
        }
    }
}
