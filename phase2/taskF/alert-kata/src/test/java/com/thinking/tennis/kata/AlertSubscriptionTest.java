package com.thinking.tennis.kata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
class AlertSubscriptionTest {
    private static final String MYSQL_IMAGE = "mysql:8.4.8";
    private static final int ROUNDS = 100;
    private static final int WORKERS = 2;
    private static final int TIMEOUT_SECONDS = 15;
    private static final long NANOS_PER_MICROSECOND = 1_000;
    private static boolean safeToReset = true;
    private static final LocalDate PLAY_DATE = LocalDate.of(2026, 10, 10);
    private static final SubscribeCommand SAME_REQUEST =
            new SubscribeCommand(1, 10, PLAY_DATE, "09:00-10:00");

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer(MYSQL_IMAGE)
            .withDatabaseName("alert_kata")
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
    AlertService service;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ApplicationContext context;

    @BeforeEach
    void prepareCommittedFixtures() {
        assertTrue(safeToReset, "Previous workers did not stop; fixture reset is forbidden");
        assertTrue(!TransactionSynchronizationManager.isActualTransactionActive(),
                "Fixture must not run in a test transaction");
        assertTrue(AopUtils.isAopProxy(service), "Call the transactional service proxy");
        var repositories = context.getBeansOfType(AlertRepository.class);
        assertEquals(1, repositories.size(), "Only the real repository should be registered");
        assertEquals(AlertRepository.class, AopUtils.getTargetClass(repositories.values().iterator().next()));
        // 테스트 메서드에 트랜잭션을 걸지 않아 준비 SQL은 작업 스레드 시작 전에 커밋된다.
        jdbc.update("DELETE FROM alert_subscriptions");
        jdbc.update("DELETE FROM kata_users");
        jdbc.update("INSERT INTO kata_users (id, version) VALUES (1, 0), (2, 0)");
    }

    @Test
    void singleRequestCreatesOneActiveSubscription() {
        long id = service.subscribe(SAME_REQUEST);
        assertTrue(id > 0);
        assertEquals(1, activeCount(SAME_REQUEST));
    }

    @Test
    void sequentialDuplicateReturnsTheExistingSubscription() {
        long first = service.subscribe(SAME_REQUEST);
        long second = service.subscribe(SAME_REQUEST);
        assertEquals(first, second);
        assertEquals(1, activeCount(SAME_REQUEST));
    }

    @Test
    void differentUsersCanSubscribeToTheSameCondition() {
        var otherUser = new SubscribeCommand(2, 10, PLAY_DATE, "09:00-10:00");
        assertNotEquals(service.subscribe(SAME_REQUEST), service.subscribe(otherUser));
        assertEquals(1, activeCount(SAME_REQUEST));
        assertEquals(1, activeCount(otherUser));
    }

    @Test
    void sameUserCanSubscribeToDifferentConditions() {
        var otherSlot = new SubscribeCommand(1, 10, PLAY_DATE, "10:00-11:00");
        assertNotEquals(service.subscribe(SAME_REQUEST), service.subscribe(otherSlot));
        assertEquals(1, activeCount(SAME_REQUEST));
        assertEquals(1, activeCount(otherSlot));
    }

    @Test
    void endedSubscriptionDoesNotPreventANewSubscription() {
        long endedId = service.subscribe(SAME_REQUEST);
        jdbc.update("UPDATE alert_subscriptions SET status = 'ENDED' WHERE id = ?", endedId);
        long newId = service.subscribe(SAME_REQUEST);
        assertNotEquals(endedId, newId);
        assertEquals(1, activeCount(SAME_REQUEST));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM alert_subscriptions", Integer.class));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Scenario.class)
    @Tag("kata-red")
    void repeatedRequestsMustRespectTheSubscriptionContract(Scenario scenario) throws Exception {
        logDatabase();
        int normalRounds = 0;
        int duplicateRounds = 0;
        int errorRounds = 0;
        int contractViolationRounds = 0;
        int overlappingRounds = 0;
        int completedRounds = 0;
        List<SubscribeCommand> commands = scenario == Scenario.DIFFERENT_USERS
                ? List.of(SAME_REQUEST, new SubscribeCommand(2, 10, PLAY_DATE, "09:00-10:00"))
                : List.of(SAME_REQUEST, SAME_REQUEST);
        ExecutorService executor = Executors.newFixedThreadPool(WORKERS);
        safeToReset = false;
        try {
            for (int round = 1; round <= ROUNDS; round++) {
                jdbc.update("DELETE FROM alert_subscriptions");
                Long seededId = scenario == Scenario.EXISTING_TOGETHER
                        ? awaitResult(executor.submit(() -> service.subscribe(SAME_REQUEST))) : null;
                List<RequestResult> results = runRequests(executor, scenario, commands);
                long committed = results.stream().filter(result -> result.error() == null).count();
                Map<SubscribeCommand, List<Long>> rows = activeIdsByCondition(commands);
                boolean duplicate = rows.values().stream().anyMatch(ids -> ids.size() > 1);
                boolean requestError = committed != WORKERS;
                boolean contractViolation = violatesContract(commands, results, rows, seededId);
                boolean overlap = callsOverlap(results);
                if (duplicate) duplicateRounds++;
                if (requestError) errorRounds++;
                if (contractViolation) contractViolationRounds++;
                if (overlap) overlappingRounds++;
                if (!requestError && !contractViolation) normalRounds++;
                completedRounds++;
                long startGapUs = Math.abs(results.get(0).started() - results.get(1).started()) / NANOS_PER_MICROSECOND;
                System.out.printf("ROUND scenario=%s round=%d requests=%d committed=%d expectedPerCondition=1 duplicate=%s requestError=%s contractViolation=%s overlap=%s startGapUs=%d rows=%s seededId=%s results=%s%n",
                        scenario, round, WORKERS, committed, duplicate, requestError, contractViolation,
                        overlap, startGapUs, rows, seededId, results);
            }
        } catch (Exception | AssertionError failure) {
            System.out.printf("ABORT scenario=%s completed=%d planned=%d cause=%s%n", scenario, completedRounds, ROUNDS, failure);
            throw failure;
        } finally {
            executor.shutdownNow();
            safeToReset = executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertTrue(safeToReset,
                    "Workers must stop before another fixture is prepared");
        }
        System.out.printf("SUMMARY scenario=%s completed=%d requestsPerRound=%d normal=%d duplicates=%d requestErrors=%d contractViolations=%d overlappingCalls=%d%n",
                scenario, completedRounds, WORKERS, normalRounds, duplicateRounds, errorRounds, contractViolationRounds, overlappingRounds);
        int errors = errorRounds;
        int violations = contractViolationRounds;
        int duplicates = duplicateRounds;
        // 첫 중복에서 멈추면 전체 시행 중 발생 횟수를 알 수 없으므로 마지막에 판정한다.
        assertAll(
                () -> assertEquals(0, errors, "Request error rounds"),
                () -> assertEquals(0, violations, "Contract violation rounds"),
                () -> assertEquals(0, duplicates, "Duplicate rounds out of " + ROUNDS));
    }

    private List<RequestResult> runRequests(ExecutorService executor, Scenario scenario,
                                           List<SubscribeCommand> commands) throws Exception {
        if (scenario == Scenario.NEW_SEQUENTIAL) {
            return List.of(
                    awaitResult(executor.submit(() -> invokeRequest(1, commands.get(0)))),
                    awaitResult(executor.submit(() -> invokeRequest(2, commands.get(1)))));
        }
        CountDownLatch ready = new CountDownLatch(WORKERS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<RequestResult>> futures = new ArrayList<>();
        try {
            for (int request = 1; request <= WORKERS; request++) {
                int requestId = request;
                futures.add(executor.submit(() -> {
                    if (scenario != Scenario.NEW_UNCOORDINATED) {
                        ready.countDown();
                        if (!start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Request start timed out");
                        }
                    }
                    return invokeRequest(requestId, commands.get(requestId - 1));
                }));
            }
            if (scenario != Scenario.NEW_UNCOORDINATED) {
                assertTrue(ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "Workers must be ready");
            }
            start.countDown();
            List<RequestResult> results = new ArrayList<>();
            for (var future : futures) {
                results.add(awaitResult(future));
            }
            return results;
        } catch (Exception | AssertionError failure) {
            futures.forEach(future -> future.cancel(true));
            throw failure;
        } finally {
            start.countDown();
        }
    }

    private <T> T awaitResult(Future<T> future) throws Exception {
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception failure) {
            future.cancel(true);
            throw failure;
        }
    }

    private RequestResult invokeRequest(int requestId, SubscribeCommand command) {
        long started = System.nanoTime();
        try {
            // Spring 프록시 호출이 반환되면 서비스 트랜잭션의 커밋도 끝난 상태다.
            long id = service.subscribe(command);
            return new RequestResult(requestId, started, System.nanoTime(), id, null);
        } catch (Exception error) {
            return new RequestResult(requestId, started, System.nanoTime(), null, error);
        }
    }

    private Map<SubscribeCommand, List<Long>> activeIdsByCondition(List<SubscribeCommand> commands) {
        Map<SubscribeCommand, List<Long>> rows = new LinkedHashMap<>();
        for (SubscribeCommand command : commands.stream().distinct().toList()) {
            rows.put(command, jdbc.queryForList("""
                    SELECT id FROM alert_subscriptions
                    WHERE user_id = ? AND court_id = ? AND play_date = ? AND time_slot = ? AND status = 'WATCHING'
                    ORDER BY id
                    """, Long.class, command.userId(), command.courtId(), command.playDate(), command.timeSlot()));
        }
        return rows;
    }

    private boolean violatesContract(List<SubscribeCommand> commands, List<RequestResult> results,
                                     Map<SubscribeCommand, List<Long>> rows, Long seededId) {
        if (rows.values().stream().anyMatch(ids -> ids.size() != 1)) return true;
        int total = jdbc.queryForObject("SELECT COUNT(*) FROM alert_subscriptions", Integer.class);
        if (total != rows.size()) return true;
        for (int index = 0; index < results.size(); index++) {
            RequestResult result = results.get(index);
            if (result.error() == null && !rows.get(commands.get(index)).contains(result.alertId())) return true;
            if (result.error() == null && seededId != null && !seededId.equals(result.alertId())) return true;
        }
        return false;
    }

    private boolean callsOverlap(List<RequestResult> results) {
        return Math.max(results.get(0).started(), results.get(1).started())
                < Math.min(results.get(0).ended(), results.get(1).ended());
    }

    private int activeCount(SubscribeCommand command) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM alert_subscriptions
                WHERE user_id = ? AND court_id = ? AND play_date = ? AND time_slot = ? AND status = 'WATCHING'
                """, Integer.class, command.userId(), command.courtId(), command.playDate(), command.timeSlot());
    }

    private void logDatabase() {
        var database = jdbc.queryForMap("SELECT VERSION() AS version, @@session.transaction_isolation AS isolation_level");
        var engines = jdbc.queryForList("""
                SELECT TABLE_NAME, ENGINE FROM information_schema.tables
                WHERE table_schema = DATABASE() ORDER BY TABLE_NAME
                """);
        assertTrue(database.get("version").toString().startsWith("8.4."));
        assertEquals(expectedIsolationLevel(), database.get("isolation_level"));
        assertEquals(2, engines.size());
        engines.forEach(table -> assertEquals("InnoDB", table.get("ENGINE")));
        System.out.printf("ENV image=%s java=%s database=%s tables=%s%n",
                MYSQL_IMAGE, System.getProperty("java.version"), database, engines);
    }

    private String expectedIsolationLevel() {
        String configured = System.getProperty("spring.datasource.hikari.transaction-isolation", "TRANSACTION_REPEATABLE_READ");
        return configured
                .replace("TRANSACTION_", "")
                .replace('_', '-')
                .toUpperCase();
    }

    enum Scenario {
        NEW_SEQUENTIAL, NEW_TOGETHER, NEW_UNCOORDINATED, EXISTING_TOGETHER, DIFFERENT_USERS
    }

    private record RequestResult(int request, long started, long ended, Long alertId, Exception error) {
    }
}
