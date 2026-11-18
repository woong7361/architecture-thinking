package com.thinking.tennis.kata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
import org.junit.jupiter.params.ParameterizedTest;
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
class TtlOverrunTest {
    private static final String MYSQL_IMAGE = "mysql:8.4.8";
    private static final String REDIS_IMAGE = "redis:7.4-alpine";
    private static final int ROUNDS = 2;
    private static final int WAITERS = 10;
    private static final int KILL_REPETITIONS = 3;
    private static final long KILL_LEASE_MILLIS = 3000;
    private static final int TIMEOUT_SECONDS = 180;
    private static final Duration RETRY_INTERVAL = Duration.ofMillis(50);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(100);
    private static final LocalDate PLAY_DATE = LocalDate.of(2026, 9, 4);
    private static final String TIME_SLOT = "10:00-11:00";
    private static final SubscribeCommand REQUEST = new SubscribeCommand(1, 7, PLAY_DATE, TIME_SLOT);

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
        for (int index = 0; index <= WAITERS; index++) {
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

    @ParameterizedTest(name = "TTL {0}ms, 반복 시도")
    @ValueSource(longs = {1000, 3000, 10000, 30000})
    void retryingWaitersWaitAsLongAsTheLease(long ttlMillis) throws Exception {
        Duration lease = Duration.ofMillis(ttlMillis);
        List<Double> lockoutMillis = new ArrayList<>();
        List<Double> longestWaitMillis = new ArrayList<>();
        int completed = 0;

        ExecutorService executor = Executors.newFixedThreadPool(WAITERS);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                prepareZombie(lease);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<SubscribeOutcome>> futures = new ArrayList<>();
                for (int waiter = 1; waiter <= WAITERS; waiter++) {
                    DistributedAlertService service = service(
                            connections.get(waiter), lease, lease.plusSeconds(5), LockedSectionHook.NONE);
                    futures.add(executor.submit(() -> {
                        assertTrue(start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                        return service.subscribe(REQUEST);
                    }));
                }
                start.countDown();

                double shortest = Double.MAX_VALUE;
                double longest = 0;
                for (Future<SubscribeOutcome> future : futures) {
                    SubscribeOutcome outcome = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    if (outcome.status() == SubscribeOutcome.Status.COMPLETED) {
                        completed++;
                    }
                    shortest = Math.min(shortest, outcome.waitMillis());
                    longest = Math.max(longest, outcome.waitMillis());
                }
                lockoutMillis.add(shortest);
                longestWaitMillis.add(longest);
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }

        System.out.printf("TTL_OVERRUN_RETRY ttlMs=%d rounds=%d waiters=%d completed=%d "
                        + "lockoutMillisPerRound=%s longestWaitPerRound=%s%n",
                ttlMillis, ROUNDS, WAITERS, completed, format(lockoutMillis), format(longestWaitMillis));

        assertEquals(ROUNDS * WAITERS, completed, "기한이 지나면 모든 대기 호출이 결국 성공해야 한다");
        for (double lockout : lockoutMillis) {
            assertTrue(lockout <= ttlMillis + 1000,
                    "잠김 시간은 기한보다 크게 길어지지 않아야 한다: " + lockout);
        }
    }

    @ParameterizedTest(name = "TTL {0}ms, 즉시 실패")
    @ValueSource(longs = {1000, 3000, 10000, 30000})
    void immediateFailurePolicyRejectsEveryRequestUntilExpiry(long ttlMillis) throws Exception {
        Duration lease = Duration.ofMillis(ttlMillis);
        List<Integer> rejections = new ArrayList<>();
        List<Double> lockoutMillis = new ArrayList<>();

        for (int round = 0; round < ROUNDS; round++) {
            prepareZombie(lease);
            DistributedAlertService service = service(
                    connections.get(1), lease, Duration.ZERO, LockedSectionHook.NONE);
            long started = System.nanoTime();
            int rejected = 0;
            while (true) {
                SubscribeOutcome outcome = service.subscribe(REQUEST);
                if (outcome.status() == SubscribeOutcome.Status.COMPLETED) {
                    break;
                }
                rejected++;
                TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL.toMillis());
            }
            rejections.add(rejected);
            lockoutMillis.add((System.nanoTime() - started) / 1_000_000.0);
        }

        System.out.printf("TTL_OVERRUN_FAILFAST ttlMs=%d rounds=%d pollIntervalMs=%d rejectionsPerRound=%s "
                        + "lockoutMillisPerRound=%s%n",
                ttlMillis, ROUNDS, POLL_INTERVAL.toMillis(), rejections, format(lockoutMillis));

        for (int rejected : rejections) {
            assertTrue(rejected > 0, "기한이 남아 있는 동안의 요청은 거절되어야 한다");
        }
    }

    @Test
    void forciblyKilledHolderLeavesLockUntilExpiry() throws Exception {
        List<Double> lockoutMillis = new ArrayList<>();
        List<Long> remainingAtKillMillis = new ArrayList<>();
        RedisSpinLock prober = new RedisSpinLock(
                connections.get(0).sync(), Duration.ofSeconds(5), RETRY_INTERVAL, ReleaseMode.TOKEN_CHECKED);

        for (int repetition = 0; repetition < KILL_REPETITIONS; repetition++) {
            connections.get(0).sync().flushall();
            String key = AlertLockKey.of(REQUEST);
            Process holder = startHolder(key);
            try {
                long acquiredAtEpoch = readAcquiredEpochMillis(holder);
                holder.destroyForcibly();
                assertTrue(holder.waitFor(30, TimeUnit.SECONDS), "강제 종료한 프로세스는 종료되어야 한다");
                long killedAt = System.nanoTime();
                long killedAtEpoch = System.currentTimeMillis();
                remainingAtKillMillis.add(KILL_LEASE_MILLIS - (killedAtEpoch - acquiredAtEpoch));

                while (prober.tryAcquire(key).isEmpty()) {
                    TimeUnit.MILLISECONDS.sleep(10);
                }
                lockoutMillis.add((System.nanoTime() - killedAt) / 1_000_000.0);
            } finally {
                holder.destroyForcibly();
            }
        }

        System.out.printf("TTL_OVERRUN_KILL leaseMs=%d repetitions=%d remainingAtKillMs=%s lockoutMillis=%s%n",
                KILL_LEASE_MILLIS, KILL_REPETITIONS, remainingAtKillMillis, format(lockoutMillis));

        for (double lockout : lockoutMillis) {
            assertTrue(lockout <= KILL_LEASE_MILLIS + 1000,
                    "강제 종료 후에도 기한이 지나면 락은 풀려야 한다: " + lockout);
            assertTrue(lockout > 0, "강제 종료 직후에는 락이 남아 있어야 한다");
        }
    }

    private Process startHolder(String key) throws Exception {
        String java = System.getProperty("java.home") + "/bin/java";
        ProcessBuilder builder = new ProcessBuilder(
                java,
                "-cp", System.getProperty("java.class.path"),
                ZombieLockHolder.class.getName(),
                REDIS.getHost(),
                Integer.toString(REDIS.getMappedPort(6379)),
                Long.toString(KILL_LEASE_MILLIS),
                key);
        builder.redirectErrorStream(true);
        return builder.start();
    }

    private long readAcquiredEpochMillis(Process holder) throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(holder.getInputStream(), StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("ACQUIRED ")) {
                return Long.parseLong(line.split(" ")[1]);
            }
        }
        throw new IllegalStateException("별도 프로세스가 락을 잡지 못했다");
    }

    private void prepareZombie(Duration lease) throws Exception {
        jdbc.update("DELETE FROM alert_subscriptions");
        connections.get(0).sync().flushall();
        DistributedAlertService zombie = service(
                connections.get(0), lease, Duration.ZERO, skipReleaseHook());
        SubscribeOutcome outcome = zombie.subscribe(REQUEST);
        assertEquals(SubscribeOutcome.Status.COMPLETED, outcome.status());
        assertNull(outcome.releaseResult(), "좀비 소유자는 해제 명령을 보내지 않는다");
    }

    private LockedSectionHook skipReleaseHook() {
        return new LockedSectionHook() {
            @Override
            public boolean skipRelease() {
                return true;
            }
        };
    }

    private DistributedAlertService service(StatefulRedisConnection<String, String> connection, Duration lease,
                                            Duration acquireWait, LockedSectionHook hook) {
        RedisSpinLock lock = new RedisSpinLock(
                connection.sync(), lease, RETRY_INTERVAL, ReleaseMode.TOKEN_CHECKED);
        return new DistributedAlertService(alertService, lock, acquireWait, Duration.ZERO, hook);
    }

    private String format(List<Double> values) {
        return values.stream().map(value -> String.format("%.1f", value)).toList().toString();
    }
}
