package com.thinking.tennis.kata;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

// 고정 간격과 지수 증가를 여러 설정으로 돌려, 같은 지연에서 어느 쪽이 시도를 덜 쓰는지 본다.
@SpringBootTest
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
class RetryWaitCurveTest {
    private static final String MYSQL_IMAGE = "mysql:8.4.8";
    private static final int ROUNDS = 5;
    private static final int WORKERS = 30;
    private static final int TIMEOUT_SECONDS = 180;
    private static final int MAX_ATTEMPTS = 60;
    private static final Duration BUDGET = Duration.ofSeconds(60);
    private static final int REPORTED_DEPTHS = 8;
    private static final LocalDate PLAY_DATE = LocalDate.of(2026, 10, 10);
    private static final SubscribeCommand SAME_REQUEST =
            new SubscribeCommand(1, 10, PLAY_DATE, "09:00-10:00");

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer(MYSQL_IMAGE)
            .withDatabaseName("alert_curve_kata")
            .withUsername("kata")
            .withPassword(requiredPassword());

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "40");
    }

    private static String requiredPassword() {
        String password = System.getenv("KATA_DB_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Set KATA_DB_PASSWORD to your chosen disposable test password");
        }
        return password;
    }

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    OptimisticSubscriptionAttempt attempt;

    @BeforeEach
    void resetFixtures() {
        jdbc.update("DELETE FROM alert_subscriptions");
        jdbc.update("DELETE FROM kata_users");
        jdbc.update("INSERT INTO kata_users (id, version) VALUES (1, 0), (2, 0)");
    }

    static Stream<Setting> conditions() {
        // 고정 간격은 상한값을 그대로 쓰므로 상한을 바꿔 대기 크기를 옮긴다.
        Stream<Setting> fixed = Stream.of(250, 500, 1000, 2000, 4000)
                .map(cap -> new Setting(RetryBackoff.FIXED_JITTER,
                        Duration.ofMillis(1), Duration.ofMillis(cap)));
        // 지수 증가는 출발값을 바꿔 대기 크기를 옮기고 상한은 2초로 고정한다.
        Stream<Setting> exponential = Stream.of(50, 100, 200, 400)
                .map(base -> new Setting(RetryBackoff.FULL_JITTER,
                        Duration.ofMillis(base), Duration.ofSeconds(2)));
        return Stream.concat(fixed, exponential);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("conditions")
    void traceWaitVersusAttempts(Setting setting) throws Exception {
        Queue<long[]> timeline = new ConcurrentLinkedQueue<>();
        RetryAttemptHook hook = new RetryAttemptHook() {
            @Override
            public void attempted(int attemptNumber, long nanoTime) {
                timeline.add(new long[] {attemptNumber, nanoTime});
            }
        };
        RetryPolicy policy = new RetryPolicy(setting.backoff(), setting.base(), setting.cap(),
                BUDGET, MAX_ATTEMPTS);
        OptimisticAlertService service = new OptimisticAlertService(attempt, policy, hook);
        ExecutorService executor = Executors.newFixedThreadPool(WORKERS);

        long bucketNanos = Math.max(1_000_000L, setting.cap().toNanos() / 20);
        List<Double> requestMillis = new ArrayList<>();
        List<Double> peakPerMean = new ArrayList<>();
        List<Double> emptyRatios = new ArrayList<>();
        long[] attemptsByDepth = new long[MAX_ATTEMPTS + 2];
        int duplicateRounds = 0;
        try {
            for (int round = 1; round <= ROUNDS; round++) {
                jdbc.update("DELETE FROM alert_subscriptions");
                timeline.clear();
                List<Callable<Long>> calls = IntStream.range(0, WORKERS)
                        .<Callable<Long>>mapToObj(index -> () -> {
                            long started = System.nanoTime();
                            try {
                                service.subscribe(SAME_REQUEST);
                            } catch (RuntimeException ignored) {
                                // 최종 실패도 걸린 시간은 센다.
                            }
                            return System.nanoTime() - started;
                        })
                        .toList();
                List<Future<Long>> futures = executor.invokeAll(calls, TIMEOUT_SECONDS, TimeUnit.SECONDS);
                for (Future<Long> future : futures) {
                    Long elapsed = elapsed(future);
                    if (elapsed != null) {
                        requestMillis.add(elapsed / 1_000_000.0);
                    }
                }

                List<long[]> entries = new ArrayList<>(timeline);
                for (long[] entry : entries) {
                    attemptsByDepth[(int) Math.min(entry[0], MAX_ATTEMPTS + 1)]++;
                }
                List<Long> retries = entries.stream()
                        .filter(entry -> entry[0] > 1)
                        .map(entry -> entry[1])
                        .sorted()
                        .toList();
                if (!retries.isEmpty()) {
                    int buckets = (int) ((retries.get(retries.size() - 1) - retries.get(0)) / bucketNanos) + 1;
                    int[] counts = new int[buckets];
                    for (long stamp : retries) {
                        counts[(int) ((stamp - retries.get(0)) / bucketNanos)]++;
                    }
                    int peak = IntStream.of(counts).max().orElse(0);
                    long empty = IntStream.of(counts).filter(count -> count == 0).count();
                    peakPerMean.add(peak / ((double) retries.size() / buckets));
                    emptyRatios.add((double) empty / buckets);
                }

                if (activeCount() > 1) {
                    duplicateRounds++;
                }
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }

        RetryMetrics metrics = service.metrics();
        int requests = ROUNDS * WORKERS;
        StringBuilder depths = new StringBuilder();
        for (int depth = 1; depth <= REPORTED_DEPTHS; depth++) {
            depths.append(depth == 1 ? "" : ",").append(attemptsByDepth[depth]);
        }
        System.out.printf("CURVE backoff=%s baseMs=%d capMs=%d rounds=%d workers=%d attempts=%d "
                        + "conflictRate=%.4f perRequestWaitMs=%.1f requestP50Ms=%.1f requestP95Ms=%.1f "
                        + "retryPeakPerMean=%.2f emptyBucketRatio=%.3f exhausted=%d duplicateRounds=%d "
                        + "attemptsByDepth=%s%n",
                setting.backoff(), setting.base().toMillis(), setting.cap().toMillis(), ROUNDS, WORKERS,
                metrics.attempts(), metrics.conflictRate(),
                metrics.totalWait().toNanos() / 1_000_000.0 / requests,
                percentile(requestMillis, 50), percentile(requestMillis, 95),
                median(peakPerMean), median(emptyRatios), metrics.exhausted(), duplicateRounds, depths);

        assertTrue(duplicateRounds == 0, setting + " produced duplicate rows");
    }

    private Long elapsed(Future<Long> future) {
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception error) {
            return null;
        }
    }

    private int activeCount() {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM alert_subscriptions
                WHERE user_id = ? AND court_id = ? AND play_date = ? AND time_slot = ? AND status = 'WATCHING'
                """, Integer.class, SAME_REQUEST.userId(), SAME_REQUEST.courtId(),
                SAME_REQUEST.playDate(), SAME_REQUEST.timeSlot());
    }

    private double median(List<Double> values) {
        return percentile(values, 50);
    }

    private double percentile(List<Double> values, int percent) {
        if (values.isEmpty()) {
            return 0.0;
        }
        List<Double> sorted = values.stream().sorted().toList();
        int index = (int) Math.ceil(sorted.size() * percent / 100.0) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    record Setting(RetryBackoff backoff, Duration base, Duration cap) {
        @Override
        public String toString() {
            return backoff + " base=" + base.toMillis() + "ms cap=" + cap.toMillis() + "ms";
        }
    }
}
