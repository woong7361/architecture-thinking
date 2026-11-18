package com.thinking.tennis.kata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

// 이 클래스는 JVM 안의 첫 호출만 관찰한다. 다른 테스트와 같은 JVM에서 실행하면 이미 준비가 끝난 상태가 되므로 따로 실행한다.
@SpringBootTest
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
class TtlFirstCallTest {
    private static final String MYSQL_IMAGE = "mysql:8.4.8";
    private static final String REDIS_IMAGE = "redis:7.4-alpine";
    private static final Duration LEASE = Duration.ofMillis(100);
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
    private static StatefulRedisConnection<String, String> connection;

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
        connection = redisClient.connect();
        assertEquals("PONG", connection.sync().ping());
    }

    @AfterAll
    static void closeRedis() {
        if (connection != null) {
            connection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown(Duration.ofSeconds(5), Duration.ofSeconds(5));
        }
    }

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    AlertService alertService;

    @Test
    void firstCallOfTheJvmCanOutlastAShortLease() throws Exception {
        jdbc.update("DELETE FROM alert_subscriptions");
        jdbc.update("DELETE FROM kata_users");
        jdbc.update("INSERT INTO kata_users (id, version) VALUES (1, 0)");
        connection.sync().flushall();

        RedisSpinLock lock = new RedisSpinLock(
                connection.sync(), LEASE, RETRY_INTERVAL, ReleaseMode.TOKEN_CHECKED);
        DistributedAlertService service = new DistributedAlertService(
                alertService, lock, Duration.ofSeconds(5), Duration.ZERO, LockedSectionHook.NONE);

        SubscribeOutcome first = service.subscribe(new SubscribeCommand(1, 7, PLAY_DATE, TIME_SLOT));
        jdbc.update("DELETE FROM alert_subscriptions");
        connection.sync().flushall();
        SubscribeOutcome second = service.subscribe(new SubscribeCommand(1, 7, PLAY_DATE, TIME_SLOT));

        System.out.printf("TTL_FIRST_CALL leaseMs=%d firstWorkMs=%.3f firstOwnedAtExit=%s firstRelease=%s "
                        + "secondWorkMs=%.3f secondOwnedAtExit=%s secondRelease=%s%n",
                LEASE.toMillis(), first.workMillis(), first.ownedAtExit(), first.releaseResult(),
                second.workMillis(), second.ownedAtExit(), second.releaseResult());

        assertEquals(SubscribeOutcome.Status.COMPLETED, first.status());
        assertEquals(SubscribeOutcome.Status.COMPLETED, second.status());
        assertTrue(second.ownedAtExit(), "준비가 끝난 뒤의 호출은 100ms 안에 끝나 소유권을 유지해야 한다");
    }
}
