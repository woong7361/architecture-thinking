package com.thinking.tennis.kata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class RedisPubSubQueueWaitComparisonTest {
    private static final String REDIS_IMAGE = "redis:7.4-alpine";
    private static final int CONTENDERS = 100;
    private static final int LOSERS = 99;
    private static final int ROUNDS = 20;
    private static final long HOLD_MILLIS = 20;
    private static final int TIMEOUT_SECONDS = 15;

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379);

    private static RedisClient client;
    private static StatefulRedisConnection<String, String> publisher;

    @BeforeAll
    static void connectRedis() {
        RedisURI uri = RedisURI.Builder.redis(REDIS.getHost(), REDIS.getMappedPort(6379)).build();
        client = RedisClient.create(uri);
        publisher = client.connect();
        assertEquals("PONG", publisher.sync().ping());
    }

    @AfterAll
    static void closeRedis() {
        if (publisher != null) {
            publisher.close();
        }
        if (client != null) {
            client.shutdown(Duration.ofSeconds(5), Duration.ofSeconds(5));
        }
    }

    @Test
    void redisPubSubDeliversOneWinnerToNinetyNineLosers() throws Exception {
        List<Double> roundMillis = new ArrayList<>();
        List<Integer> messageCounts = new ArrayList<>();
        long bytesInBefore = redisStat("total_net_input_bytes");
        long bytesOutBefore = redisStat("total_net_output_bytes");
        for (int round = 0; round < ROUNDS; round++) {
            RedisRound result = runRound();
            assertEquals(1, result.winners());
            assertEquals(LOSERS, result.losers());
            assertEquals(CONTENDERS, result.messagesDelivered());
            assertEquals(1, result.distinctWinnerIds());
            roundMillis.add(result.wallMillis());
            messageCounts.add(result.messagesDelivered());
        }
        long bytesInAfter = redisStat("total_net_input_bytes");
        long bytesOutAfter = redisStat("total_net_output_bytes");
        System.out.printf("REDIS_QUEUE_SUMMARY image=%s rounds=%d contenders=%d winners=1 losers=99 "
                        + "medianRoundMs=%.3f messages=%d redisInputBytes=%d redisOutputBytes=%d "
                        + "subscriberConnections=%d%n",
                REDIS_IMAGE, ROUNDS, CONTENDERS, median(roundMillis), messageCounts.stream().mapToInt(Integer::intValue).sum(),
                bytesInAfter - bytesInBefore, bytesOutAfter - bytesOutBefore, ROUNDS * CONTENDERS);
    }

    private RedisRound runRound() throws Exception {
        AtomicInteger winner = new AtomicInteger(-1);
        AtomicReference<Integer> finalWinner = new AtomicReference<>();
        AtomicInteger messagesDelivered = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(CONTENDERS);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(CONTENDERS);
        List<Future<ParticipantResult>> futures = new ArrayList<>();
        String channel = "kata:queue:" + UUID.randomUUID();
        long started = System.nanoTime();
        try {
            for (int participant = 0; participant < CONTENDERS; participant++) {
                int participantId = participant;
                futures.add(executor.submit(() -> participate(
                        channel, participantId, winner, finalWinner, messagesDelivered, ready, start)));
            }
            assertTrue(ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "All Redis subscribers must be ready");
            start.countDown();
            List<ParticipantResult> results = new ArrayList<>();
            for (Future<ParticipantResult> future : futures) {
                results.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            assertTrue(results.stream().allMatch(result -> result.winnerId() == finalWinner.get()));
            return new RedisRound(
                    (int) results.stream().filter(ParticipantResult::won).count(),
                    (int) results.stream().filter(result -> !result.won()).count(),
                    messagesDelivered.get(),
                    (int) results.stream().map(ParticipantResult::winnerId).distinct().count(),
                    nanosToMillis(System.nanoTime() - started));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
    }

    private ParticipantResult participate(
            String channel,
            int participantId,
            AtomicInteger winner,
            AtomicReference<Integer> finalWinner,
            AtomicInteger messagesDelivered,
            CountDownLatch ready,
            CountDownLatch start) throws Exception {
        StatefulRedisPubSubConnection<String, String> subscriber = client.connectPubSub();
        CompletableFuture<Integer> message = new CompletableFuture<>();
        subscriber.addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String receivedChannel, String receivedMessage) {
                if (channel.equals(receivedChannel)) {
                    messagesDelivered.incrementAndGet();
                    message.complete(Integer.parseInt(receivedMessage));
                }
            }
        });
        try {
            subscriber.sync().subscribe(channel);
            ready.countDown();
            assertTrue(start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            boolean won = winner.compareAndSet(-1, participantId);
            int winnerId;
            if (won) {
                TimeUnit.MILLISECONDS.sleep(HOLD_MILLIS);
                winnerId = participantId;
                finalWinner.set(participantId);
                publisher.sync().publish(channel, Integer.toString(participantId));
            }
            winnerId = message.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return new ParticipantResult(won, winnerId);
        } finally {
            subscriber.close();
        }
    }

    private long redisStat(String key) {
        String info = publisher.sync().info("stats");
        return info.lines()
                .filter(line -> line.startsWith(key + ":"))
                .map(line -> line.substring(key.length() + 1).trim())
                .mapToLong(Long::parseLong)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing Redis stat: " + key));
    }

    private double median(List<Double> values) {
        List<Double> sorted = values.stream().sorted().toList();
        return sorted.get((sorted.size() - 1) / 2);
    }

    private double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private record ParticipantResult(boolean won, int winnerId) {
    }

    private record RedisRound(int winners, int losers, int messagesDelivered,
                              int distinctWinnerIds, double wallMillis) {
    }
}
