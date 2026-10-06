package com.thinking.tennis.kata;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class OptimisticAlertService {
    private static final int MAX_ATTEMPTS = 5;

    private final OptimisticSubscriptionAttempt attempt;
    private final AtomicLong conflictCount = new AtomicLong();

    public OptimisticAlertService(OptimisticSubscriptionAttempt attempt) {
        this.attempt = attempt;
    }

    public long subscribe(SubscribeCommand command) {
        for (int attemptNumber = 1; attemptNumber <= MAX_ATTEMPTS; attemptNumber++) {
            try {
                return attempt.subscribe(command);
            } catch (OptimisticConflictException conflict) {
                conflictCount.incrementAndGet();
                if (attemptNumber == MAX_ATTEMPTS) {
                    throw conflict;
                }
            }
        }
        throw new IllegalStateException("Unreachable retry state");
    }

    public long conflictCount() {
        return conflictCount.get();
    }

    public void resetMetrics() {
        conflictCount.set(0);
    }
}
