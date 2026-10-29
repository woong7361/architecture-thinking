package com.thinking.tennis.kata;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/**
 * claim-09 가 `뒤집히는 값` 으로 적은 문장을 측정한다.
 *
 * "동시 요청이 셋 이상이면 밀린 쪽이 다시 밀리는 연쇄가 생겨 시도 수가 예산에 닿는다."
 *
 * F-3 의 측정은 요청 둘뿐이었고 회차당 충돌이 1회였다. 요청 수를 늘려 가며 충돌 수와 요청 오류를
 * 센다. 단언하지 않고 숫자만 찍는다. 이 테스트는 판정 장치가 아니라 관찰 도구다.
 */
@SpringBootTest
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
class RetryBudgetProbeTest {
    private static final String MYSQL_IMAGE = "mysql:8.4.8";
    private static final int ROUNDS = 30;
    private static final int TIMEOUT_SECONDS = 60;
    private static final int[] WORKER_COUNTS = {2, 3, 5, 8};
    private static final LocalDate PLAY_DATE = LocalDate.of(2026, 10, 10);
    private static final SubscribeCommand SAME_REQUEST =
            new SubscribeCommand(1, 10, PLAY_DATE, "09:00-10:00");

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer(MYSQL_IMAGE)
            .withDatabaseName("alert_retry_probe")
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
    OptimisticAlertService optimistic;

    @BeforeEach
    void resetFixtures() {
        jdbc.update("DELETE FROM alert_subscriptions");
        jdbc.update("DELETE FROM kata_users");
        jdbc.update("INSERT INTO kata_users (id, version) VALUES (1, 0), (2, 0)");
    }

    @Test
    void conflictsAndErrorsByWorkerCount() throws Exception {
        System.out.printf("PROBE rounds=%d maxAttempts=5%n", ROUNDS);
        for (int workers : WORKER_COUNTS) {
            long conflicts = 0;
            int errors = 0;
            int duplicates = 0;

            for (int round = 0; round < ROUNDS; round++) {
                jdbc.update("DELETE FROM alert_subscriptions");
                jdbc.update("UPDATE kata_users SET version = 0 WHERE id = 1");
                optimistic.resetMetrics();

                ExecutorService pool = Executors.newFixedThreadPool(workers);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<Long>> futures = new ArrayList<>();
                try {
                    for (int worker = 0; worker < workers; worker++) {
                        futures.add(pool.submit(() -> {
                            start.await();
                            return optimistic.subscribe(SAME_REQUEST);
                        }));
                    }
                    start.countDown();
                    for (Future<Long> future : futures) {
                        try {
                            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        } catch (Exception failure) {
                            errors++;
                        }
                    }
                } finally {
                    pool.shutdownNow();
                }

                conflicts += optimistic.conflictCount();
                Integer rows = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM alert_subscriptions WHERE status = 'WATCHING'", Integer.class);
                if (rows != null && rows > 1) {
                    duplicates++;
                }
            }

            System.out.printf(
                    "PROBE workers=%d rounds=%d conflictsTotal=%d conflictsPerRound=%.2f "
                            + "conflictsPerRequest=%.2f requestErrors=%d duplicateRounds=%d%n",
                    workers, ROUNDS, conflicts,
                    (double) conflicts / ROUNDS,
                    (double) conflicts / (ROUNDS * workers),
                    errors, duplicates);
        }
    }
}
