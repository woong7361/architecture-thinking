package com.thinking.tennis.kata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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
class TtlUnderrunTest {
    private static final String MYSQL_IMAGE = "mysql:8.4.8";
    private static final String REDIS_IMAGE = "redis:7.4-alpine";
    private static final int ROUNDS = 20;
    private static final int WORKERS = 2;
    private static final int TIMEOUT_SECONDS = 60;
    private static final Duration INJECTED_DELAY = Duration.ofMillis(300);
    private static final Duration ACQUIRE_WAIT = Duration.ofSeconds(5);
    private static final Duration SLOW_RETRY = Duration.ofMillis(10);
    private static final Duration FAST_RETRY = Duration.ofMillis(1);
    private static final LocalDate PLAY_DATE = LocalDate.of(2026, 9, 4);
    private static final String TIME_SLOT = "10:00-11:00";
    private static final SubscribeCommand SAME_REQUEST = new SubscribeCommand(1, 7, PLAY_DATE, TIME_SLOT);

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
        for (int index = 0; index < WORKERS; index++) {
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
        jdbc.update("INSERT INTO kata_users (id, version) VALUES (1, 0)");
        connections.get(0).sync().flushall();
    }

    @ParameterizedTest(name = "TTL {0}ms, 지연 300ms")
    @ValueSource(longs = {100, 200, 300, 500, 1000, 3000})
    void sweepTtlWithInjectedDelay(long ttlMillis) throws Exception {
        Observation observation = run(ttlMillis, INJECTED_DELAY, SLOW_RETRY, ReleaseMode.TOKEN_CHECKED);
        print("TTL_UNDERRUN_DELAYED", ttlMillis, INJECTED_DELAY.toMillis(), ReleaseMode.TOKEN_CHECKED, observation);

        if (ttlMillis >= 1000) {
            assertEquals(1, observation.maxInSection(),
                    "작업 시간보다 충분히 긴 TTL에서는 임계 구역에 한 번에 하나만 들어가야 한다");
        }
        assertEquals(ROUNDS * WORKERS, observation.calls(), "모든 호출의 결과를 수집해야 한다");
    }

    @ParameterizedTest(name = "TTL {0}ms, 지연 없음")
    @ValueSource(longs = {5, 10, 20, 50, 200})
    void sweepTtlWithoutInjectedDelay(long ttlMillis) throws Exception {
        Observation observation = run(ttlMillis, Duration.ZERO, FAST_RETRY, ReleaseMode.TOKEN_CHECKED);
        print("TTL_UNDERRUN_PLAIN", ttlMillis, 0, ReleaseMode.TOKEN_CHECKED, observation);

        if (ttlMillis >= 200) {
            assertEquals(1, observation.maxInSection(),
                    "지연이 없을 때 200ms TTL은 작업 시간보다 길어야 한다");
        }
        assertEquals(ROUNDS * WORKERS, observation.calls(), "모든 호출의 결과를 수집해야 한다");
    }

    @ParameterizedTest(name = "해제 방식 {0}, TTL {1}ms")
    @CsvSource({"TOKEN_CHECKED,100", "UNCONDITIONAL,100", "TOKEN_CHECKED,200", "UNCONDITIONAL,200"})
    void compareReleaseModesUnderExpiry(ReleaseMode releaseMode, long ttlMillis) throws Exception {
        Observation observation = run(ttlMillis, INJECTED_DELAY, SLOW_RETRY, releaseMode);
        print("TTL_RELEASE_MODE", ttlMillis, INJECTED_DELAY.toMillis(), releaseMode, observation);

        if (releaseMode == ReleaseMode.TOKEN_CHECKED) {
            assertEquals(0, observation.releasedForeign(), "토큰을 확인하는 해제는 남의 락을 지우지 않는다");
        }
        assertEquals(ROUNDS * WORKERS, observation.calls(), "모든 호출의 결과를 수집해야 한다");
    }

    private Observation run(long ttlMillis, Duration delay, Duration retryInterval, ReleaseMode releaseMode)
            throws Exception {
        Duration lease = Duration.ofMillis(ttlMillis);
        int maxInSection = 0;
        int concurrentRounds = 0;
        int duplicateRounds = 0;
        int distinctIdRounds = 0;
        int calls = 0;
        AtomicInteger ownershipLost = new AtomicInteger();
        AtomicInteger notAcquired = new AtomicInteger();
        Map<ReleaseResult, Integer> releaseCounts = new EnumMap<>(ReleaseResult.class);

        ExecutorService executor = Executors.newFixedThreadPool(WORKERS);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                jdbc.update("DELETE FROM alert_subscriptions");
                connections.get(0).sync().flushall();
                SectionTracker tracker = new SectionTracker();
                CountDownLatch start = new CountDownLatch(1);
                List<Future<SubscribeOutcome>> futures = new ArrayList<>();
                for (int worker = 0; worker < WORKERS; worker++) {
                    DistributedAlertService service = service(
                            connections.get(worker), lease, retryInterval, releaseMode, tracker, delay, ownershipLost);
                    futures.add(executor.submit(() -> {
                        assertTrue(start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                        return service.subscribe(SAME_REQUEST);
                    }));
                }
                start.countDown();

                List<Long> returnedIds = new ArrayList<>();
                for (Future<SubscribeOutcome> future : futures) {
                    SubscribeOutcome outcome = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    calls++;
                    if (outcome.status() == SubscribeOutcome.Status.NOT_ACQUIRED) {
                        notAcquired.incrementAndGet();
                    }
                    if (outcome.subscriptionId() != null) {
                        returnedIds.add(outcome.subscriptionId());
                    }
                    if (outcome.releaseResult() != null) {
                        releaseCounts.merge(outcome.releaseResult(), 1, Integer::sum);
                    }
                }

                maxInSection = Math.max(maxInSection, tracker.max());
                if (tracker.max() > 1) {
                    concurrentRounds++;
                }
                if (activeRows() > 1) {
                    duplicateRounds++;
                }
                if (returnedIds.stream().distinct().count() > 1) {
                    distinctIdRounds++;
                }
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }

        return new Observation(calls, maxInSection, concurrentRounds, duplicateRounds, distinctIdRounds,
                ownershipLost.get(), notAcquired.get(),
                releaseCounts.getOrDefault(ReleaseResult.RELEASED_OWN, 0),
                releaseCounts.getOrDefault(ReleaseResult.RELEASED_FOREIGN, 0),
                releaseCounts.getOrDefault(ReleaseResult.NOT_RELEASED, 0),
                releaseCounts.getOrDefault(ReleaseResult.KEY_ABSENT, 0));
    }

    private int activeRows() {
        Integer rows = jdbc.queryForObject("""
                SELECT COUNT(*) FROM alert_subscriptions
                WHERE user_id = ? AND court_id = ? AND play_date = ? AND time_slot = ? AND status = 'WATCHING'
                """, Integer.class, SAME_REQUEST.userId(), SAME_REQUEST.courtId(), PLAY_DATE, TIME_SLOT);
        return rows == null ? 0 : rows;
    }

    private DistributedAlertService service(StatefulRedisConnection<String, String> connection, Duration lease,
                                            Duration retryInterval, ReleaseMode releaseMode, SectionTracker tracker,
                                            Duration delay, AtomicInteger ownershipLost) {
        RedisSpinLock lock = new RedisSpinLock(connection.sync(), lease, retryInterval, releaseMode);
        return new DistributedAlertService(alertService, lock, ACQUIRE_WAIT, Duration.ZERO,
                new LockedSectionHook() {
                    @Override
                    public void entered(LockToken token) {
                        tracker.enter();
                        if (!delay.isZero()) {
                            sleepQuietly(delay);
                        }
                    }

                    @Override
                    public void beforeExit(LockToken token, boolean ownedAtExit) {
                        if (!ownedAtExit) {
                            ownershipLost.incrementAndGet();
                        }
                        tracker.exit();
                    }
                });
    }

    private static void sleepQuietly(Duration delay) {
        try {
            TimeUnit.MILLISECONDS.sleep(delay.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    private void print(String tag, long ttlMillis, long delayMillis, ReleaseMode releaseMode,
                       Observation observation) {
        System.out.printf("%s ttlMs=%d delayMs=%d release=%s rounds=%d calls=%d maxInSection=%d "
                        + "concurrentRounds=%d duplicateRounds=%d distinctIdRounds=%d ownershipLost=%d "
                        + "notAcquired=%d releasedOwn=%d releasedForeign=%d notReleased=%d keyAbsent=%d%n",
                tag, ttlMillis, delayMillis, releaseMode, ROUNDS, observation.calls(), observation.maxInSection(),
                observation.concurrentRounds(), observation.duplicateRounds(), observation.distinctIdRounds(),
                observation.ownershipLost(), observation.notAcquired(), observation.releasedOwn(),
                observation.releasedForeign(), observation.notReleased(), observation.keyAbsent());
    }

    private static final class SectionTracker {
        private final AtomicInteger current = new AtomicInteger();
        private final AtomicInteger max = new AtomicInteger();

        void enter() {
            int now = current.incrementAndGet();
            max.accumulateAndGet(now, Math::max);
        }

        void exit() {
            current.decrementAndGet();
        }

        int max() {
            return max.get();
        }
    }

    private record Observation(
            int calls,
            int maxInSection,
            int concurrentRounds,
            int duplicateRounds,
            int distinctIdRounds,
            int ownershipLost,
            int notAcquired,
            int releasedOwn,
            int releasedForeign,
            int notReleased,
            int keyAbsent) {
    }
}
