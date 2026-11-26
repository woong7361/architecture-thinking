package com.thinking.tennis.kata;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Locale;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// 갱신 경로와 발송 경로를 같은 순간에 호출한다. 하네스는 그대로 두고 창구의 획득 순서만 바꿔 전후를 비교한다.
@Testcontainers
class CrossResourceDeadlockTest {
    private static final String REDIS_IMAGE = "redis:7.4-alpine";
    private static final Duration LEASE = Duration.ofSeconds(1);
    private static final Duration RETRY_INTERVAL = Duration.ofMillis(20);
    private static final Duration BUDGET = Duration.ofSeconds(5);
    private static final Duration READ_STEP = Duration.ofMillis(20);
    private static final LocalDate BASE_DATE = LocalDate.of(2026, 11, 20);
    private static final long COURT_ID = 7L;
    private static final String TIME_SLOT = "19:00";
    private static final int ROUNDS = 5;

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379);

    private static RedisClient client;
    private static StatefulRedisConnection<String, String> refreshConnection;
    private static StatefulRedisConnection<String, String> dispatchConnection;

    @BeforeAll
    static void connectRedis() {
        RedisURI uri = RedisURI.Builder.redis(REDIS.getHost(), REDIS.getMappedPort(6379)).build();
        client = RedisClient.create(uri);
        refreshConnection = client.connect();
        dispatchConnection = client.connect();
        assertEquals("PONG", refreshConnection.sync().ping());
    }

    @AfterAll
    static void closeRedis() {
        if (refreshConnection != null) {
            refreshConnection.close();
        }
        if (dispatchConnection != null) {
            dispatchConnection.close();
        }
        if (client != null) {
            client.shutdown(Duration.ofSeconds(5), Duration.ofSeconds(5));
        }
    }

    @ParameterizedTest
    @EnumSource(LockOrder.class)
    void globalOrderRemovesTheCycleThatRequestedOrderProduces(LockOrder order) throws Exception {
        AvailabilityRefreshService refresh =
                new AvailabilityRefreshService(facade(refreshConnection, order), readStep());
        AlertDispatchService dispatch =
                new AlertDispatchService(facade(dispatchConnection, order), readStep());
        ExecutorService pool = Executors.newFixedThreadPool(2);

        int stalledRounds = 0;
        int ownershipLostRounds = 0;
        int violationRounds = 0;
        int completedPaths = 0;
        try {
            for (int round = 1; round <= ROUNDS; round++) {
                LocalDate playDate = BASE_DATE.plusDays(round);
                SubscribeCommand watching = new SubscribeCommand(1L, COURT_ID, playDate, TIME_SLOT);
                CyclicBarrier sameMoment = new CyclicBarrier(2);

                Future<LockedOutcome> refreshPath = pool.submit(() -> {
                    sameMoment.await();
                    return refresh.refresh(COURT_ID, playDate, TIME_SLOT);
                });
                Future<LockedOutcome> dispatchPath = pool.submit(() -> {
                    sameMoment.await();
                    return dispatch.dispatch(watching);
                });

                LockedOutcome refreshOutcome = refreshPath.get(30, TimeUnit.SECONDS);
                LockedOutcome dispatchOutcome = dispatchPath.get(30, TimeUnit.SECONDS);
                boolean stalled = stalledUntilLeaseExpiry(refreshOutcome) && stalledUntilLeaseExpiry(dispatchOutcome);
                boolean ownershipLost = lostOwnership(refreshOutcome) || lostOwnership(dispatchOutcome);
                boolean violated = refreshOutcome.status() == LockedOutcome.Status.ORDER_VIOLATION
                        || dispatchOutcome.status() == LockedOutcome.Status.ORDER_VIOLATION;
                stalledRounds += stalled ? 1 : 0;
                ownershipLostRounds += ownershipLost ? 1 : 0;
                violationRounds += violated ? 1 : 0;
                completedPaths += completed(refreshOutcome) + completed(dispatchOutcome);

                System.out.printf(Locale.ROOT,
                        "FACADE_ROUND order=%s round=%d lease=%dms refreshLongestWait=%.1fms "
                                + "dispatchLongestWait=%.1fms stalled=%s ownershipLost=%s status=%s,%s%n",
                        order, round, LEASE.toMillis(), longestWait(refreshOutcome), longestWait(dispatchOutcome),
                        stalled, ownershipLost, refreshOutcome.status(), dispatchOutcome.status());
            }
        } finally {
            pool.shutdownNow();
        }

        int stalls = stalledRounds;
        int ownershipLosses = ownershipLostRounds;
        int violations = violationRounds;
        int completed = completedPaths;
        System.out.printf(Locale.ROOT,
                "FACADE_SUMMARY order=%s rounds=%d stalledRounds=%d ownershipLostRounds=%d "
                        + "orderViolationRounds=%d completedPaths=%d/%d%n",
                order, ROUNDS, stalls, ownershipLosses, violations, completed, ROUNDS * 2);

        if (order == LockOrder.AS_REQUESTED) {
            assertAll(
                    () -> assertTrue(stalls > 0, "순서를 그대로 두었는데 서로를 기다린 회차가 없다: " + stalls),
                    () -> assertTrue(ownershipLosses > 0, "소유권을 잃은 회차가 없다: " + ownershipLosses));
            return;
        }

        assertAll(
                () -> assertEquals(0, stalls, "전역 순서인데 기한까지 기다린 회차가 있다"),
                () -> assertEquals(0, ownershipLosses, "전역 순서인데 소유권을 잃은 회차가 있다"),
                () -> assertEquals(0, violations, "전역 순서를 위반한 요청이 있다"),
                () -> assertEquals(ROUNDS * 2, completed, "끝까지 수행하지 못한 경로가 있다"));
    }

    private static boolean stalledUntilLeaseExpiry(LockedOutcome outcome) {
        return longestWait(outcome) >= LEASE.toMillis() * 0.9;
    }

    private static double longestWait(LockedOutcome outcome) {
        return outcome.steps().stream().mapToDouble(LockStep::waitMillis).max().orElse(0);
    }

    private static boolean lostOwnership(LockedOutcome outcome) {
        return outcome.steps().stream().anyMatch(step -> !step.allPreviousHeldAtAcquire());
    }

    private static int completed(LockedOutcome outcome) {
        return outcome.status() == LockedOutcome.Status.COMPLETED ? 1 : 0;
    }

    private static LockPathSteps readStep() {
        return new LockPathSteps() {
            // 외부 예약처 조회와 저장된 현황 읽기에 걸리는 시간만 흉내낸다.
            @Override
            public void afterFirstLocked() throws InterruptedException {
                TimeUnit.MILLISECONDS.sleep(READ_STEP.toMillis());
            }

            @Override
            public void afterBothLocked() throws InterruptedException {
                TimeUnit.MILLISECONDS.sleep(READ_STEP.toMillis());
            }
        };
    }

    private OrderedLockFacade facade(StatefulRedisConnection<String, String> connection, LockOrder order) {
        RedisSpinLock lock = new RedisSpinLock(connection.sync(), LEASE, RETRY_INTERVAL, ReleaseMode.TOKEN_CHECKED);
        return new OrderedLockFacade(lock, order, BUDGET);
    }
}
