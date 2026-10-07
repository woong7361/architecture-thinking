package com.thinking.tennis.kata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class QueueWaitComparisonTest {
    private static final int CONTENDERS = 100;
    private static final int LOSERS = 99;
    private static final int ROUNDS = 20;
    private static final long HOLD_MILLIS = 20;
    private static final int TIMEOUT_SECONDS = 15;

    @ParameterizedTest(name = "{0}")
    @EnumSource(WaitStrategy.class)
    void exactlyOneWinnerAndNinetyNineLosers(WaitStrategy strategy) throws Exception {
        List<Double> roundMillis = new ArrayList<>();
        List<Double> spinChecks = new ArrayList<>();
        List<Double> pubSubWaits = new ArrayList<>();
        for (int round = 0; round < ROUNDS; round++) {
            BenchmarkRound result = runRound(strategy);
            assertEquals(1, result.winners(), strategy + " must have one winner");
            assertEquals(LOSERS, result.losers(), strategy + " must have ninety-nine losers");
            assertEquals(1, result.distinctWinnerIds(), strategy + " must publish one final winner");
            roundMillis.add(result.wallMillis());
            spinChecks.add((double) result.spinChecks());
            pubSubWaits.add((double) result.pubSubWaits());
        }

        System.out.printf("QUEUE_SUMMARY strategy=%s rounds=%d contenders=%d winners=1 losers=99 "
                        + "medianRoundMs=%.3f medianSpinChecks=%.0f "
                        + "medianPubSubWaits=%.0f%n",
                strategy, ROUNDS, CONTENDERS, median(roundMillis), median(spinChecks), median(pubSubWaits));
    }

    private BenchmarkRound runRound(WaitStrategy strategy) throws Exception {
        AtomicInteger winner = new AtomicInteger(-1);
        AtomicReference<Integer> finalWinner = new AtomicReference<>();
        AtomicLong spinChecks = new AtomicLong();
        AtomicLong pubSubWaits = new AtomicLong();
        ConditionPubSub signal = new ConditionPubSub();
        CountDownLatch ready = new CountDownLatch(CONTENDERS);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(CONTENDERS);
        List<Future<ParticipantResult>> futures = new ArrayList<>();
        long roundStarted = System.nanoTime();
        try {
            for (int participant = 0; participant < CONTENDERS; participant++) {
                int participantId = participant;
                futures.add(executor.submit(() -> participate(
                        strategy, participantId, winner, finalWinner, signal, ready, start,
                        spinChecks, pubSubWaits)));
            }
            assertTrue(ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "All contenders must be ready");
            start.countDown();
            List<ParticipantResult> results = new ArrayList<>();
            for (Future<ParticipantResult> future : futures) {
                results.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            long roundNanos = System.nanoTime() - roundStarted;
            long publishedWinner = finalWinner.get();
            assertTrue(results.stream().allMatch(result -> result.winnerId() == publishedWinner));
            return new BenchmarkRound(
                    (int) results.stream().filter(ParticipantResult::won).count(),
                    (int) results.stream().filter(result -> !result.won()).count(),
                    (int) results.stream().map(ParticipantResult::winnerId).distinct().count(),
                    nanosToMillis(roundNanos),
                    spinChecks.get(),
                    pubSubWaits.get());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
    }

    private ParticipantResult participate(
            WaitStrategy strategy,
            int participantId,
            AtomicInteger winner,
            AtomicReference<Integer> finalWinner,
            ConditionPubSub signal,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicLong spinChecks,
            AtomicLong pubSubWaits) throws InterruptedException {
        if (strategy == WaitStrategy.PUB_SUB) {
            signal.subscribe();
        }
        ready.countDown();
        start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        boolean won = winner.compareAndSet(-1, participantId);
        int winnerId;
        if (won) {
            TimeUnit.MILLISECONDS.sleep(HOLD_MILLIS);
            winnerId = participantId;
            finalWinner.set(participantId);
            if (strategy == WaitStrategy.PUB_SUB) {
                signal.publish(participantId);
            } else {
                finalWinner.set(participantId);
            }
        } else if (strategy == WaitStrategy.PUB_SUB) {
            winnerId = signal.awaitWinner(pubSubWaits);
        } else {
            while (finalWinner.get() == null) {
                spinChecks.incrementAndGet();
                Thread.onSpinWait();
            }
            winnerId = finalWinner.get();
        }
        return new ParticipantResult(won, winnerId);
    }

    private double median(List<Double> values) {
        List<Double> sorted = values.stream().sorted().toList();
        return sorted.get((sorted.size() - 1) / 2);
    }

    private double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    enum WaitStrategy {
        SPIN, PUB_SUB
    }

    private static final class ConditionPubSub {
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition published = lock.newCondition();
        private int subscribers;
        private Integer winner;

        void subscribe() {
            lock.lock();
            try {
                subscribers++;
            } finally {
                lock.unlock();
            }
        }

        void publish(int winnerId) {
            lock.lock();
            try {
                if (subscribers < LOSERS) {
                    throw new IllegalStateException("All subscribers must register before publish");
                }
                winner = winnerId;
                published.signalAll();
            } finally {
                lock.unlock();
            }
        }

        int awaitWinner(AtomicLong waitCount) throws InterruptedException {
            lock.lock();
            try {
                while (winner == null) {
                    waitCount.incrementAndGet();
                    published.await();
                }
                return winner;
            } finally {
                lock.unlock();
            }
        }
    }

    private record ParticipantResult(boolean won, int winnerId) {
    }

    private record BenchmarkRound(
            int winners,
            int losers,
            int distinctWinnerIds,
            double wallMillis,
            long spinChecks,
            long pubSubWaits) {
    }
}
