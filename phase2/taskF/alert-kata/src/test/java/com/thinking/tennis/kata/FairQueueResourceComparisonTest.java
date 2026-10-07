package com.thinking.tennis.kata;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.management.OperatingSystemMXBean;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class FairQueueResourceComparisonTest {
    private static final int PARTICIPANTS = 100;
    private static final int ROUNDS = 50;
    private static final int WARMUP_ROUNDS = 10;
    private static final int TRIALS = 3;
    private static final int HOLD_MILLIS = 20;
    private static final int TIMEOUT_SECONDS = 15;
    private static final double NANOS_PER_MILLI = 1_000_000.0;
    private static final String CHANNEL = "kata:fair:result";
    private static final String IMAGE = "redis:7.4-alpine@sha256:858f009f9709ce576febc734aa78b8f6d624b82571f9ddb6bda4377c833b3499";

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(IMAGE).withExposedPorts(6379);

    private final OperatingSystemMXBean cpu = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private final List<StatefulRedisPubSubConnection<String, String>> subscribers = new ArrayList<>();
    private volatile RoundState currentRound;
    private StatefulRedisConnection<String, String> publisher;
    private int sequence;

    @Test
    void compareEqualWorkWithReusedWorkersAndConnections() throws Exception {
        assertTrue(cpu.getProcessCpuTime() >= 0, "Process CPU time must be supported");
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(PARTICIPANTS);
        RedisClient client = RedisClient.create(RedisURI.create(REDIS.getHost(), REDIS.getMappedPort(6379)));
        long setupStarted = System.nanoTime();
        try {
            executor.prestartAllCoreThreads();
            publisher = client.connect();
            connectSubscribers(client);
            double setupMs = millis(System.nanoTime() - setupStarted);
            Map<String, String> server = info();
            System.out.printf(Locale.ROOT,
                    "FAIR_ENV java=%s os=%s logicalCpus=%d redis=%s image=%s participants=%d rounds=%d trials=%d warmup=%d holdMs=%d payloadBytes=%d workerThreads=%d subscriberConnections=%d publisherConnections=1 setupMs=%.3f%n",
                    System.getProperty("java.version"), System.getProperty("os.name"), cpu.getAvailableProcessors(),
                    server.get("redis_version"), IMAGE, PARTICIPANTS, ROUNDS, TRIALS, WARMUP_ROUNDS,
                    HOLD_MILLIS, payload(1, 1).length(), executor.getPoolSize(), subscribers.size(), setupMs);
            for (Strategy strategy : Strategy.values()) {
                for (int round = 0; round < WARMUP_ROUNDS; round++) runRound(executor, strategy);
            }
            // 순서를 회전해 특정 방식만 JVM 초기화나 앞선 부하의 영향을 받지 않게 한다.
            for (int trial = 0; trial < TRIALS; trial++) {
                for (int offset = 0; offset < Strategy.values().length; offset++) {
                    Strategy strategy = Strategy.values()[(trial + offset) % Strategy.values().length];
                    measureBatch(executor, strategy, trial + 1);
                }
            }
        } finally {
            executor.shutdownNow();
            boolean stopped = executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            subscribers.forEach(StatefulRedisPubSubConnection::close);
            if (publisher != null) publisher.close();
            client.shutdown();
            assertTrue(stopped, "Workers must terminate even after a failed round");
        }
    }

    private void connectSubscribers(RedisClient client) {
        for (int participant = 0; participant < PARTICIPANTS; participant++) {
            int recipient = participant;
            var subscriber = client.connectPubSub();
            subscribers.add(subscriber);
            subscriber.addListener(new RedisPubSubAdapter<String, String>() {
                @Override
                public void message(String channel, String message) {
                    RoundState state = currentRound;
                    if (CHANNEL.equals(channel) && state != null && state.strategy == Strategy.REDIS) {
                        state.receipts.get(recipient).complete(message);
                    }
                }
            });
            // 동기 SUBSCRIBE 응답으로 등록 완료를 확인한 뒤 측정을 시작한다.
            subscriber.sync().subscribe(CHANNEL);
        }
    }

    private void measureBatch(ExecutorService executor, Strategy strategy, int trial) throws Exception {
        Map<String, String> before = info();
        long cpuBefore = cpu.getProcessCpuTime();
        long started = System.nanoTime();
        List<RoundResult> rounds = new ArrayList<>();
        for (int round = 0; round < ROUNDS; round++) rounds.add(runRound(executor, strategy));
        double elapsedMs = millis(System.nanoTime() - started);
        double javaCpuMs = millis(cpu.getProcessCpuTime() - cpuBefore);
        Map<String, String> after = info();
        double redisCpuMs = (number(after, "used_cpu_user") + number(after, "used_cpu_sys")
                - number(before, "used_cpu_user") - number(before, "used_cpu_sys")) * 1_000;
        double javaOneCorePct = 100 * javaCpuMs / elapsedMs;
        double redisOneCorePct = 100 * redisCpuMs / elapsedMs;
        System.out.printf(Locale.ROOT,
                "FAIR_SUMMARY trial=%d strategy=%s rounds=%d requests=%d winners=%d losers=%d observations=%d elapsedMs=%.3f medianRoundMs=%.3f p95RoundMs=%.3f javaCpuMs=%.3f redisCpuMs=%.3f javaOneCorePct=%.3f redisOneCorePct=%.3f combinedCapacityPct=%.3f redisInputBytes=%d redisOutputBytes=%d spinChecks=%d conditionAwaits=%d%n",
                trial, strategy, ROUNDS, ROUNDS * PARTICIPANTS, ROUNDS, ROUNDS * (PARTICIPANTS - 1),
                ROUNDS * PARTICIPANTS, elapsedMs, median(rounds.stream().map(RoundResult::wallMs).toList()),
                percentile95(rounds.stream().map(RoundResult::wallMs).toList()), javaCpuMs, redisCpuMs,
                javaOneCorePct, redisOneCorePct, (javaOneCorePct + redisOneCorePct) / cpu.getAvailableProcessors(),
                integer(after, "total_net_input_bytes") - integer(before, "total_net_input_bytes"),
                integer(after, "total_net_output_bytes") - integer(before, "total_net_output_bytes"),
                rounds.stream().mapToLong(RoundResult::spinChecks).sum(),
                rounds.stream().mapToLong(RoundResult::conditionAwaits).sum());
        for (int round = 0; round < rounds.size(); round++) {
            RoundResult result = rounds.get(round);
            System.out.printf(Locale.ROOT,
                    "FAIR_ROUND trial=%d strategy=%s round=%d winners=1 losers=99 observations=100 wallMs=%.6f spinChecks=%d conditionAwaits=%d%n",
                    trial, strategy, round + 1, result.wallMs(), result.spinChecks(), result.conditionAwaits());
        }
    }

    private RoundResult runRound(ExecutorService executor, Strategy strategy) throws Exception {
        RoundState state = new RoundState(++sequence, strategy);
        currentRound = state;
        CountDownLatch ready = new CountDownLatch(PARTICIPANTS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ParticipantResult>> futures = new ArrayList<>();
        long started = System.nanoTime();
        try {
            for (int participant = 0; participant < PARTICIPANTS; participant++) {
                int id = participant;
                futures.add(executor.submit(() -> participate(state, id, ready, start)));
            }
            assertTrue(ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "All 100 workers must be ready");
            start.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
            List<ParticipantResult> results = new ArrayList<>();
            for (var future : futures) {
                results.add(future.get(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS));
            }
            double wallMs = millis(System.nanoTime() - started);
            assertEquals(1, results.stream().filter(ParticipantResult::won).count());
            assertEquals(PARTICIPANTS - 1, results.stream().filter(result -> !result.won()).count());
            String expected = payload(state.sequence, state.winner.get());
            assertTrue(results.stream().allMatch(result -> expected.equals(result.message())), "Every result must match this round's winner");
            return new RoundResult(wallMs, results.stream().mapToLong(ParticipantResult::spinChecks).sum(),
                    results.stream().mapToLong(ParticipantResult::conditionAwaits).sum());
        } finally {
            start.countDown();
            futures.forEach(future -> { if (!future.isDone()) future.cancel(true); });
        }
    }

    private ParticipantResult participate(RoundState state, int id, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) throw new TimeoutException("Start timed out");
        boolean won = state.winner.compareAndSet(-1, id);
        if (won) {
            // 실제 업무 속도를 재현하는 값이 아니라 세 방식에 같은 대기 조건을 주는 값이다.
            TimeUnit.MILLISECONDS.sleep(HOLD_MILLIS);
            String message = payload(state.sequence, id);
            switch (state.strategy) {
                case SPIN -> state.message = message;
                case CONDITION -> {
                    state.lock.lock();
                    try {
                        state.message = message;
                        state.completed.signalAll();
                    } finally { state.lock.unlock(); }
                }
                case REDIS -> assertEquals(PARTICIPANTS, publisher.sync().publish(CHANNEL, message));
            }
        }
        long checks = 0;
        long waits = 0;
        String message;
        switch (state.strategy) {
            case SPIN -> {
                // 공유 카운터는 측정 대상에 없는 경합을 만들므로 스레드별로 센다.
                while ((message = state.message) == null) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                    checks++;
                    Thread.onSpinWait();
                }
            }
            case CONDITION -> {
                long remaining = TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
                state.lock.lockInterruptibly();
                try {
                    while (state.message == null) {
                        if (remaining <= 0) throw new TimeoutException("Condition timed out");
                        waits++;
                        remaining = state.completed.awaitNanos(remaining);
                    }
                    message = state.message;
                } finally { state.lock.unlock(); }
            }
            case REDIS -> message = state.receipts.get(id).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            default -> throw new IllegalStateException();
        }
        return new ParticipantResult(won, message, checks, waits);
    }

    private Map<String, String> info() {
        Map<String, String> fields = new HashMap<>();
        publisher.sync().info().lines().filter(line -> line.contains(":"))
                .forEach(line -> { int split = line.indexOf(':'); fields.put(line.substring(0, split), line.substring(split + 1).trim()); });
        return fields;
    }

    private static String payload(int sequence, int winner) {
        return String.format(Locale.ROOT, "round:%08d:winner:%03d", sequence, winner);
    }

    private static double number(Map<String, String> fields, String key) { return Double.parseDouble(fields.get(key)); }
    private static long integer(Map<String, String> fields, String key) { return Long.parseLong(fields.get(key)); }
    private static double millis(long nanos) { return nanos / NANOS_PER_MILLI; }
    private static double median(List<Double> values) {
        var sorted = values.stream().sorted().toList();
        return (sorted.get((sorted.size() - 1) / 2) + sorted.get(sorted.size() / 2)) / 2;
    }
    private static double percentile95(List<Double> values) {
        return values.stream().sorted().toList().get((int) Math.ceil(values.size() * 0.95) - 1);
    }

    enum Strategy { SPIN, CONDITION, REDIS }
    private static final class RoundState {
        final int sequence;
        final Strategy strategy;
        final AtomicInteger winner = new AtomicInteger(-1);
        final ReentrantLock lock = new ReentrantLock();
        final Condition completed = lock.newCondition();
        final List<CompletableFuture<String>> receipts = new ArrayList<>();
        volatile String message;
        RoundState(int sequence, Strategy strategy) {
            this.sequence = sequence;
            this.strategy = strategy;
            for (int i = 0; i < PARTICIPANTS; i++) receipts.add(new CompletableFuture<>());
        }
    }
    private record ParticipantResult(boolean won, String message, long spinChecks, long conditionAwaits) { }
    private record RoundResult(double wallMs, long spinChecks, long conditionAwaits) { }
}
