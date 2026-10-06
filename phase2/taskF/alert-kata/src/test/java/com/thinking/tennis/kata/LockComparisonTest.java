package com.thinking.tennis.kata;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
class LockComparisonTest {
    private static final String MYSQL_IMAGE = "mysql:8.4.8";
    private static final int ROUNDS = 100;
    private static final int WORKERS = 2;
    private static final int TIMEOUT_SECONDS = 15;
    private static final LocalDate PLAY_DATE = LocalDate.of(2026, 10, 10);
    private static final SubscribeCommand SAME_REQUEST =
            new SubscribeCommand(1, 10, PLAY_DATE, "09:00-10:00");

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer(MYSQL_IMAGE)
            .withDatabaseName("alert_lock_kata")
            .withUsername("kata")
            .withPassword(requiredPassword());

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
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
    PessimisticAlertService pessimistic;
    @Autowired
    OptimisticAlertService optimistic;

    @BeforeEach
    void resetFixtures() {
        jdbc.update("DELETE FROM alert_subscriptions");
        jdbc.update("DELETE FROM kata_users");
        jdbc.update("INSERT INTO kata_users (id, version) VALUES (1, 0), (2, 0)");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(LockStrategy.class)
    void bothLockStrategiesPreserveTheOneSubscriptionContract(LockStrategy strategy) throws Exception {
        resetMetrics(strategy);
        ExecutorService executor = Executors.newFixedThreadPool(WORKERS);
        List<Double> requestMillis = new ArrayList<>();
        List<Double> roundMillis = new ArrayList<>();
        int duplicateRounds = 0;
        int errorRounds = 0;
        try {
            for (int round = 1; round <= ROUNDS; round++) {
                jdbc.update("DELETE FROM alert_subscriptions");
                long roundStarted = System.nanoTime();
                List<Future<RequestResult>> futures = List.of(
                        executor.submit(() -> invoke(strategy, 1)),
                        executor.submit(() -> invoke(strategy, 2)));
                List<RequestResult> results = futures.stream().map(this::await).toList();
                roundMillis.add(nanosToMillis(System.nanoTime() - roundStarted));
                results.forEach(result -> requestMillis.add(nanosToMillis(result.ended() - result.started())));

                int rows = activeCount(SAME_REQUEST);
                boolean duplicate = rows != 1;
                boolean error = results.stream().anyMatch(result -> result.error() != null);
                if (duplicate) duplicateRounds++;
                if (error) errorRounds++;
                int currentRound = round;
                assertAll(
                        () -> assertEquals(1, rows, strategy + " active rows at round " + currentRound),
                        () -> assertTrue(results.stream().allMatch(result -> result.error() == null),
                                strategy + " request error at round " + currentRound),
                        () -> assertEquals(1, results.stream().map(RequestResult::alertId).distinct().count(),
                                strategy + " returned different IDs at round " + currentRound));
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }

        System.out.printf("LOCK_SUMMARY strategy=%s rounds=%d duplicates=%d requestErrors=%d "
                        + "medianRequestMs=%.3f medianRoundMs=%.3f optimisticConflicts=%d%n",
                strategy, ROUNDS, duplicateRounds, errorRounds,
                median(requestMillis), median(roundMillis), conflictCount(strategy));
    }

    @Test
    void optimisticStrategyActuallyRecordsConflictsForOverlappingRequests() throws Exception {
        optimistic.resetMetrics();
        ExecutorService executor = Executors.newFixedThreadPool(WORKERS);
        try {
            List<Future<Long>> futures = List.of(
                    executor.submit(() -> optimistic.subscribe(SAME_REQUEST)),
                    executor.submit(() -> optimistic.subscribe(SAME_REQUEST)));
            futures.forEach(this::awaitValue);
            assertTrue(optimistic.conflictCount() > 0);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
    }

    private RequestResult invoke(LockStrategy strategy, int requestId) {
        long started = System.nanoTime();
        try {
            long id = subscribe(strategy, SAME_REQUEST);
            return new RequestResult(requestId, started, System.nanoTime(), id, null);
        } catch (Exception error) {
            return new RequestResult(requestId, started, System.nanoTime(), null, error);
        }
    }

    private RequestResult await(Future<RequestResult> future) {
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception error) {
            throw new AssertionError("Lock request did not finish", error);
        }
    }

    private long awaitValue(Future<Long> future) {
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception error) {
            throw new AssertionError("Optimistic request did not finish", error);
        }
    }

    private int activeCount(SubscribeCommand command) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM alert_subscriptions
                WHERE user_id = ? AND court_id = ? AND play_date = ? AND time_slot = ? AND status = 'WATCHING'
                """, Integer.class, command.userId(), command.courtId(), command.playDate(), command.timeSlot());
    }

    private double median(List<Double> values) {
        List<Double> sorted = values.stream().sorted().toList();
        return sorted.get((sorted.size() - 1) / 2);
    }

    private double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private long subscribe(LockStrategy strategy, SubscribeCommand command) {
        return switch (strategy) {
            case PESSIMISTIC -> pessimistic.subscribe(command);
            case OPTIMISTIC -> optimistic.subscribe(command);
        };
    }

    private void resetMetrics(LockStrategy strategy) {
        if (strategy == LockStrategy.OPTIMISTIC) {
            optimistic.resetMetrics();
        }
    }

    private long conflictCount(LockStrategy strategy) {
        return strategy == LockStrategy.OPTIMISTIC ? optimistic.conflictCount() : 0;
    }

    enum LockStrategy {
        PESSIMISTIC, OPTIMISTIC
    }

    private record RequestResult(int request, long started, long ended, Long alertId, Exception error) {
    }
}
