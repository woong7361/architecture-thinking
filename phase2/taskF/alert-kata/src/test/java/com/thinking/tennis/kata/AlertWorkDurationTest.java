package com.thinking.tennis.kata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
class AlertWorkDurationTest {
    private static final String MYSQL_IMAGE = "mysql:8.4.8";
    private static final String REDIS_IMAGE = "redis:7.4-alpine";
    private static final int SEQUENTIAL_CALLS = 50;
    private static final int CONCURRENT_WORKERS = 10;
    private static final int CONCURRENT_ROUNDS = 5;
    private static final int PING_SAMPLES = 200;
    private static final int TIMEOUT_SECONDS = 60;
    private static final Duration LEASE = Duration.ofSeconds(30);
    private static final Duration ACQUIRE_WAIT = Duration.ofSeconds(20);
    private static final Duration RETRY_INTERVAL = Duration.ofMillis(10);
    private static final LocalDate PLAY_DATE = LocalDate.of(2026, 9, 4);
    private static final String TIME_SLOT = "10:00-11:00";

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer(MYSQL_IMAGE)
            .withDatabaseName("alert_lock_kata")
            .withUsername("kata")
            .withPassword(requiredPassword());

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379);

    private static RedisClient redisClient;
    private static List<StatefulRedisConnection<String, String>> connections;

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

    @BeforeAll
    static void connectRedis() {
        RedisURI uri = RedisURI.Builder.redis(REDIS.getHost(), REDIS.getMappedPort(6379)).build();
        redisClient = RedisClient.create(uri);
        connections = new ArrayList<>();
        for (int index = 0; index < CONCURRENT_WORKERS; index++) {
            connections.add(redisClient.connect());
        }
        assertEquals("PONG", connections.get(0).sync().ping());
    }

    @AfterAll
    static void closeRedis() {
        if (connections != null) {
            connections.forEach(StatefulRedisConnection::close);
        }
        if (redisClient != null) {
            redisClient.shutdown(Duration.ofSeconds(5), Duration.ofSeconds(5));
        }
    }

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    AlertService alertService;

    @BeforeEach
    void resetFixtures() {
        jdbc.update("DELETE FROM alert_subscriptions");
        jdbc.update("DELETE FROM kata_users");
        for (int userId = 1; userId <= CONCURRENT_WORKERS; userId++) {
            jdbc.update("INSERT INTO kata_users (id, version) VALUES (?, 0)", userId);
        }
        connections.get(0).sync().flushall();
    }

    @Test
    void measuresCriticalSectionDurationWithoutInjectedDelay() throws Exception {
        double redisRoundTripMillis = measureRedisRoundTrip();
        List<Double> sequential = measureSequential();
        List<Double> concurrent = measureConcurrent();

        System.out.printf("TTL_WORK_ENV mysqlImage=%s redisImage=%s java=%s lease=%dms acquireWait=%dms "
                        + "redisPingMedianMs=%.3f%n",
                MYSQL_IMAGE, REDIS_IMAGE, System.getProperty("java.version"),
                LEASE.toMillis(), ACQUIRE_WAIT.toMillis(), redisRoundTripMillis);
        printSummary("sequential", sequential);
        printSummary("concurrent", concurrent);

        assertTrue(sequential.size() == SEQUENTIAL_CALLS, "순차 조건은 모든 호출이 임계 구역에 들어가야 한다");
        assertTrue(concurrent.size() == CONCURRENT_WORKERS * CONCURRENT_ROUNDS,
                "동시 조건도 모든 호출이 임계 구역에 들어가야 한다");
    }

    private List<Double> measureSequential() throws Exception {
        DistributedAlertService service = service(connections.get(0));
        List<Double> workMillis = new ArrayList<>();
        for (int call = 0; call < SEQUENTIAL_CALLS; call++) {
            jdbc.update("DELETE FROM alert_subscriptions");
            SubscribeOutcome outcome = service.subscribe(command(1));
            assertEquals(SubscribeOutcome.Status.COMPLETED, outcome.status());
            assertTrue(outcome.ownedAtExit(), "지연이 없으면 작업을 끝낸 뒤에도 락은 자기 것이어야 한다");
            workMillis.add(outcome.workMillis());
        }
        return workMillis;
    }

    private List<Double> measureConcurrent() throws Exception {
        List<Double> workMillis = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_WORKERS);
        try {
            for (int round = 0; round < CONCURRENT_ROUNDS; round++) {
                jdbc.update("DELETE FROM alert_subscriptions");
                CountDownLatch start = new CountDownLatch(1);
                List<Future<SubscribeOutcome>> futures = new ArrayList<>();
                for (int worker = 0; worker < CONCURRENT_WORKERS; worker++) {
                    int userId = worker + 1;
                    DistributedAlertService service = service(connections.get(worker));
                    futures.add(executor.submit(() -> {
                        assertTrue(start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                        return service.subscribe(command(userId));
                    }));
                }
                start.countDown();
                for (Future<SubscribeOutcome> future : futures) {
                    SubscribeOutcome outcome = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    assertEquals(SubscribeOutcome.Status.COMPLETED, outcome.status());
                    workMillis.add(outcome.workMillis());
                }
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
        return workMillis;
    }

    private double measureRedisRoundTrip() {
        List<Double> samples = new ArrayList<>();
        for (int sample = 0; sample < PING_SAMPLES; sample++) {
            long started = System.nanoTime();
            connections.get(0).sync().ping();
            samples.add((System.nanoTime() - started) / 1_000_000.0);
        }
        return percentile(samples, 50);
    }

    private DistributedAlertService service(StatefulRedisConnection<String, String> connection) {
        RedisSpinLock lock = new RedisSpinLock(
                connection.sync(), LEASE, RETRY_INTERVAL, ReleaseMode.TOKEN_CHECKED);
        return new DistributedAlertService(
                alertService, lock, ACQUIRE_WAIT, Duration.ZERO, LockedSectionHook.NONE);
    }

    private SubscribeCommand command(long userId) {
        return new SubscribeCommand(userId, 7, PLAY_DATE, TIME_SLOT);
    }

    private void printSummary(String condition, List<Double> workMillis) {
        int maxIndex = 0;
        for (int index = 1; index < workMillis.size(); index++) {
            if (workMillis.get(index) > workMillis.get(maxIndex)) {
                maxIndex = index;
            }
        }
        List<Double> withoutFirst = workMillis.subList(1, workMillis.size());
        System.out.printf("TTL_WORK_SUMMARY condition=%s calls=%d p50=%.3f p95=%.3f p99=%.3f max=%.3f "
                        + "maxCallIndex=%d firstCall=%.3f p99WithoutFirst=%.3f maxWithoutFirst=%.3f%n",
                condition, workMillis.size(), percentile(workMillis, 50), percentile(workMillis, 95),
                percentile(workMillis, 99), percentile(workMillis, 100), maxIndex, workMillis.get(0),
                percentile(withoutFirst, 99), percentile(withoutFirst, 100));
    }

    private double percentile(List<Double> values, int percentile) {
        List<Double> sorted = values.stream().sorted().toList();
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}
