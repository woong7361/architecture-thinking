package com.thinking.tennis.kata;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
class RetryJitterComparisonTest {
    private static final String MYSQL_IMAGE = "mysql:8.4.8";
    private static final int TIMEOUT_SECONDS = 180;
    private static final int MAX_ATTEMPTS = 60;
    // 시도 시각을 이 폭으로 묶어 한 구간에 몇 건이 몰렸는지 센다. 대기 규모에 맞춰 규모마다 달리 잡는다.
    private static final LocalDate PLAY_DATE = LocalDate.of(2026, 10, 10);
    private static final SubscribeCommand SAME_REQUEST =
            new SubscribeCommand(1, 10, PLAY_DATE, "09:00-10:00");

    // 호출자가 응답을 기다리는 경로. 대기 규모가 임계 구간과 같은 자리에 있다.
    private static final Scale RESPONSIVE =
            new Scale("RESPONSIVE", Duration.ofMillis(10), Duration.ofMillis(200),
                    Duration.ofSeconds(5), 20, 10_000_000L);
    // 호출자가 기다리지 않는 경로. 대기를 초 단위로 쓸 수 있다.
    private static final Scale TOLERANT =
            new Scale("TOLERANT", Duration.ofMillis(200), Duration.ofSeconds(2),
                    Duration.ofSeconds(60), 5, 200_000_000L);

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer(MYSQL_IMAGE)
            .withDatabaseName("alert_retry_kata")
            .withUsername("kata")
            .withPassword(requiredPassword());

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        // 같은 행을 기다리는 요청은 대기 중에도 커넥션을 쥔다. 풀이 좁으면 충돌율이 아니라 풀 고갈을 재게 된다.
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

    static Stream<Arguments> conditions() {
        Stream<Arguments> responsive = Stream.of(RetryBackoff.values())
                .flatMap(backoff -> IntStream.of(2, 10, 30)
                        .mapToObj(workers -> Arguments.of(RESPONSIVE, backoff, workers)));
        // 느려도 되는 규모는 한 회차가 수십 배 길어지므로 경합이 가장 큰 조건만 돌린다.
        Stream<Arguments> tolerant = Stream.of(RetryBackoff.values())
                .map(backoff -> Arguments.of(TOLERANT, backoff, 30));
        return Stream.concat(responsive, tolerant);
    }

    @ParameterizedTest(name = "{0} {1} workers={2}")
    @MethodSource("conditions")
    void measureConflictAndArrivalShape(Scale scale, RetryBackoff backoff, int workers) throws Exception {
        Queue<long[]> timeline = new ConcurrentLinkedQueue<>();
        RetryAttemptHook hook = new RetryAttemptHook() {
            @Override
            public void attempted(int attemptNumber, long nanoTime) {
                timeline.add(new long[] {attemptNumber, nanoTime});
            }
        };
        RetryPolicy policy =
                new RetryPolicy(backoff, scale.base(), scale.cap(), scale.budget(), MAX_ATTEMPTS);
        OptimisticAlertService service = new OptimisticAlertService(attempt, policy, hook);
        ExecutorService executor = Executors.newFixedThreadPool(workers);

        List<Double> roundMillis = new ArrayList<>();
        List<Double> requestMillis = new ArrayList<>();
        List<Double> peaks = new ArrayList<>();
        List<Double> peakPerMean = new ArrayList<>();
        List<Double> emptyRatios = new ArrayList<>();
        int duplicateRounds = 0;
        int failedRequests = 0;
        try {
            for (int round = 1; round <= scale.rounds(); round++) {
                jdbc.update("DELETE FROM alert_subscriptions");
                timeline.clear();
                List<Callable<RequestTiming>> calls = IntStream.range(0, workers)
                        .mapToObj(index -> timedCall(service))
                        .toList();
                long roundStarted = System.nanoTime();
                List<Future<RequestTiming>> futures = executor.invokeAll(calls, TIMEOUT_SECONDS, TimeUnit.SECONDS);
                roundMillis.add((System.nanoTime() - roundStarted) / 1_000_000.0);

                for (Future<RequestTiming> future : futures) {
                    RequestTiming timing = result(future);
                    if (timing == null || timing.failed()) {
                        failedRequests++;
                    }
                    if (timing != null) {
                        requestMillis.add(timing.elapsedNanos() / 1_000_000.0);
                    }
                }

                // 첫 시도는 전원이 동시에 출발하므로 분포에서 뺀다. 재시도가 어떻게 퍼졌는지만 본다.
                List<Long> retries = timeline.stream()
                        .filter(entry -> entry[0] > 1)
                        .map(entry -> entry[1])
                        .sorted()
                        .toList();
                RoundShape shape = shapeOf(retries, scale.bucketNanos());
                if (shape.buckets() > 0) {
                    peaks.add((double) shape.peak());
                    peakPerMean.add(shape.peak() / shape.mean());
                    emptyRatios.add(shape.emptyRatio());
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
        System.out.printf("RETRY_SUMMARY scale=%s backoff=%s workers=%d rounds=%d attempts=%d conflicts=%d "
                        + "conflictRate=%.4f exhausted=%d failedRequests=%d totalWaitMs=%.1f "
                        + "medianRoundMs=%.1f duplicateRounds=%d%n",
                scale.name(), backoff, workers, scale.rounds(), metrics.attempts(), metrics.conflicts(),
                metrics.conflictRate(), metrics.exhausted(), failedRequests,
                metrics.totalWait().toNanos() / 1_000_000.0, median(roundMillis), duplicateRounds);
        System.out.printf("RETRY_SHAPE scale=%s backoff=%s workers=%d bucketMs=%d retryPeakMedian=%.1f "
                        + "retryPeakMax=%.1f retryPeakPerMeanMedian=%.2f emptyBucketRatioMedian=%.3f "
                        + "requestP50Ms=%.1f requestP95Ms=%.1f requestMaxMs=%.1f%n",
                scale.name(), backoff, workers, scale.bucketNanos() / 1_000_000L, median(peaks), max(peaks),
                median(peakPerMean), median(emptyRatios),
                median(requestMillis), percentile(requestMillis, 95), max(requestMillis));

        int duplicates = duplicateRounds;
        assertAll(
                () -> assertTrue(duplicates == 0,
                        backoff + " produced duplicate rows in " + duplicates + " rounds"),
                () -> assertTrue(metrics.attempts() >= (long) scale.rounds() * workers,
                        backoff + " recorded fewer attempts than requests"));
    }

    private Callable<RequestTiming> timedCall(OptimisticAlertService service) {
        return () -> {
            long started = System.nanoTime();
            try {
                service.subscribe(SAME_REQUEST);
                return new RequestTiming(System.nanoTime() - started, false);
            } catch (RuntimeException error) {
                return new RequestTiming(System.nanoTime() - started, true);
            }
        };
    }

    private RequestTiming result(Future<RequestTiming> future) {
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception error) {
            return null;
        }
    }

    // 시도 시각을 고정 폭 구간으로 묶는다. 구간의 수는 그 회차가 실제로 걸린 길이가 정한다.
    private RoundShape shapeOf(List<Long> stamps, long bucketNanos) {
        if (stamps.isEmpty()) {
            return new RoundShape(0, 0, 0.0, 0.0);
        }
        long first = stamps.get(0);
        long last = stamps.get(stamps.size() - 1);
        int buckets = (int) ((last - first) / bucketNanos) + 1;
        int[] counts = new int[buckets];
        for (long stamp : stamps) {
            counts[(int) ((stamp - first) / bucketNanos)]++;
        }
        int peak = IntStream.of(counts).max().orElse(0);
        long empty = IntStream.of(counts).filter(count -> count == 0).count();
        double mean = (double) stamps.size() / buckets;
        return new RoundShape(buckets, peak, mean, (double) empty / buckets);
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

    private double max(List<Double> values) {
        return values.isEmpty() ? 0.0 : Collections.max(values);
    }

    record Scale(String name, Duration base, Duration cap, Duration budget, int rounds, long bucketNanos) {
        @Override
        public String toString() {
            return name;
        }
    }

    private record RequestTiming(long elapsedNanos, boolean failed) {
    }

    private record RoundShape(int buckets, int peak, double mean, double emptyRatio) {
    }
}
