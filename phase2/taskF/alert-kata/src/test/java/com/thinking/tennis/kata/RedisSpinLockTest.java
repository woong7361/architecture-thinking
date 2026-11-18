package com.thinking.tennis.kata;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class RedisSpinLockTest {
    private static final String REDIS_IMAGE = "redis:7.4-alpine";
    private static final Duration LEASE = Duration.ofSeconds(3);
    private static final Duration RETRY_INTERVAL = Duration.ofMillis(20);

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379);

    private static RedisClient client;
    private static StatefulRedisConnection<String, String> firstConnection;
    private static StatefulRedisConnection<String, String> secondConnection;

    private String key;

    @BeforeAll
    static void connectRedis() {
        RedisURI uri = RedisURI.Builder.redis(REDIS.getHost(), REDIS.getMappedPort(6379)).build();
        client = RedisClient.create(uri);
        firstConnection = client.connect();
        secondConnection = client.connect();
        assertEquals("PONG", firstConnection.sync().ping());
    }

    @AfterAll
    static void closeRedis() {
        if (firstConnection != null) {
            firstConnection.close();
        }
        if (secondConnection != null) {
            secondConnection.close();
        }
        if (client != null) {
            client.shutdown(Duration.ofSeconds(5), Duration.ofSeconds(5));
        }
    }

    @BeforeEach
    void useFreshKey() {
        key = "lock:test:" + UUID.randomUUID();
    }

    @Test
    void acquiresAndReleasesItsOwnLock() {
        RedisSpinLock lock = tokenCheckedLock(firstConnection, LEASE);

        LockToken token = lock.tryAcquire(key).orElseThrow();

        assertAll(
                () -> assertTrue(lock.heldBy(token)),
                () -> assertEquals(ReleaseResult.RELEASED_OWN, lock.release(token)),
                () -> assertFalse(lock.heldBy(token)));
    }

    @Test
    void secondClientCannotAcquireHeldLock() {
        RedisSpinLock first = tokenCheckedLock(firstConnection, LEASE);
        RedisSpinLock second = tokenCheckedLock(secondConnection, LEASE);

        LockToken token = first.tryAcquire(key).orElseThrow();

        assertAll(
                () -> assertTrue(second.tryAcquire(key).isEmpty()),
                () -> assertTrue(first.heldBy(token)));
    }

    @Test
    void tokenCheckedReleaseRefusesToDeleteAnotherLock() {
        RedisSpinLock owner = tokenCheckedLock(firstConnection, LEASE);
        RedisSpinLock stranger = tokenCheckedLock(secondConnection, LEASE);

        LockToken ownerToken = owner.tryAcquire(key).orElseThrow();
        LockToken strangerToken = new LockToken(key, UUID.randomUUID().toString(), LEASE);

        assertAll(
                () -> assertEquals(ReleaseResult.NOT_RELEASED, stranger.release(strangerToken)),
                () -> assertTrue(owner.heldBy(ownerToken)));
    }

    @Test
    void releaseWithoutTokenCheckDeletesAnotherLockAndReportsIt() {
        RedisSpinLock owner = tokenCheckedLock(firstConnection, LEASE);
        RedisSpinLock stranger = new RedisSpinLock(
                secondConnection.sync(), LEASE, RETRY_INTERVAL, ReleaseMode.UNCONDITIONAL);

        LockToken ownerToken = owner.tryAcquire(key).orElseThrow();
        LockToken strangerToken = new LockToken(key, UUID.randomUUID().toString(), LEASE);

        assertAll(
                () -> assertEquals(ReleaseResult.RELEASED_FOREIGN, stranger.release(strangerToken)),
                () -> assertFalse(owner.heldBy(ownerToken)));
    }

    @Test
    void expiredLeaseLetsAnotherClientAcquire() throws Exception {
        RedisSpinLock owner = tokenCheckedLock(firstConnection, Duration.ofMillis(200));
        RedisSpinLock other = tokenCheckedLock(secondConnection, LEASE);

        LockToken ownerToken = owner.tryAcquire(key).orElseThrow();
        TimeUnit.MILLISECONDS.sleep(400);
        Optional<LockToken> otherToken = other.tryAcquire(key);

        assertAll(
                () -> assertTrue(otherToken.isPresent(), "만료 후에는 다른 클라이언트가 락을 잡는다"),
                () -> assertFalse(owner.heldBy(ownerToken), "소유자는 통보 없이 소유권을 잃는다"),
                () -> assertEquals(Duration.ZERO, owner.remainingLease(ownerToken)));
    }

    @Test
    void remainingLeaseIsVisibleOnlyToTheOwner() {
        RedisSpinLock owner = tokenCheckedLock(firstConnection, LEASE);

        LockToken ownerToken = owner.tryAcquire(key).orElseThrow();
        LockToken strangerToken = new LockToken(key, UUID.randomUUID().toString(), LEASE);

        assertAll(
                () -> assertTrue(owner.remainingLease(ownerToken).toMillis() > 0),
                () -> assertTrue(owner.remainingLease(ownerToken).compareTo(LEASE) <= 0),
                () -> assertEquals(Duration.ZERO, owner.remainingLease(strangerToken)));
    }

    @Test
    void lockKeyCoversCourtAndDateAndTimeSlot() {
        SubscribeCommand first = new SubscribeCommand(1, 7, LocalDate.of(2026, 9, 4), "10:00-11:00");
        SubscribeCommand sameSlotOtherUser = new SubscribeCommand(2, 7, LocalDate.of(2026, 9, 4), "10:00-11:00");
        SubscribeCommand otherSlot = new SubscribeCommand(1, 7, LocalDate.of(2026, 9, 4), "11:00-12:00");

        assertAll(
                () -> assertEquals(AlertLockKey.of(first), AlertLockKey.of(sameSlotOtherUser)),
                () -> assertFalse(AlertLockKey.of(first).equals(AlertLockKey.of(otherSlot))));
    }

    private RedisSpinLock tokenCheckedLock(StatefulRedisConnection<String, String> connection, Duration lease) {
        RedisCommands<String, String> commands = connection.sync();
        return new RedisSpinLock(commands, lease, RETRY_INTERVAL, ReleaseMode.TOKEN_CHECKED);
    }
}
