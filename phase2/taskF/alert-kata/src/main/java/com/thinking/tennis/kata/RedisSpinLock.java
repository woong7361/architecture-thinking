package com.thinking.tennis.kata;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.sync.RedisCommands;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public final class RedisSpinLock {
    private static final String RELEASE_IF_OWNED = """
            if redis.call("get", KEYS[1]) == ARGV[1] then
                return redis.call("del", KEYS[1])
            else
                return 0
            end
            """;

    // 토큰을 확인하지 않는 해제의 비교군이다. 조회는 무엇을 지웠는지 집계하려고 넣었고, 삭제는 소유자와 무관하게 수행한다.
    private static final String RELEASE_WITHOUT_CHECK = """
            local current = redis.call("get", KEYS[1])
            if current == false then
                return 0
            end
            redis.call("del", KEYS[1])
            if current == ARGV[1] then
                return 1
            end
            return 2
            """;

    private static final String REMAINING_LEASE_IF_OWNED = """
            if redis.call("get", KEYS[1]) == ARGV[1] then
                return redis.call("pttl", KEYS[1])
            end
            return -3
            """;

    private final RedisCommands<String, String> commands;
    private final Duration lease;
    private final Duration retryInterval;
    private final ReleaseMode releaseMode;

    public RedisSpinLock(RedisCommands<String, String> commands, Duration lease, Duration retryInterval,
                         ReleaseMode releaseMode) {
        this.commands = commands;
        this.lease = lease;
        this.retryInterval = retryInterval;
        this.releaseMode = releaseMode;
    }

    public Duration lease() {
        return lease;
    }

    public Optional<LockToken> tryAcquire(String key) {
        String token = UUID.randomUUID().toString();
        String stored = commands.set(key, token, SetArgs.Builder.nx().px(lease.toMillis()));
        if (!"OK".equals(stored)) {
            return Optional.empty();
        }
        return Optional.of(new LockToken(key, token, lease));
    }

    public Optional<LockToken> acquire(String key, Duration maxWait) throws InterruptedException {
        long deadline = System.nanoTime() + maxWait.toNanos();
        while (true) {
            Optional<LockToken> acquired = tryAcquire(key);
            if (acquired.isPresent()) {
                return acquired;
            }
            if (System.nanoTime() >= deadline) {
                return Optional.empty();
            }
            Thread.sleep(retryInterval.toMillis());
        }
    }

    public ReleaseResult release(LockToken token) {
        if (releaseMode == ReleaseMode.TOKEN_CHECKED) {
            return eval(RELEASE_IF_OWNED, token) == 1 ? ReleaseResult.RELEASED_OWN : ReleaseResult.NOT_RELEASED;
        }
        return switch ((int) eval(RELEASE_WITHOUT_CHECK, token)) {
            case 1 -> ReleaseResult.RELEASED_OWN;
            case 2 -> ReleaseResult.RELEASED_FOREIGN;
            default -> ReleaseResult.KEY_ABSENT;
        };
    }

    public boolean heldBy(LockToken token) {
        return token.token().equals(commands.get(token.key()));
    }

    public Duration remainingLease(LockToken token) {
        long millis = eval(REMAINING_LEASE_IF_OWNED, token);
        return millis <= 0 ? Duration.ZERO : Duration.ofMillis(millis);
    }

    private long eval(String script, LockToken token) {
        Long code = commands.eval(script, ScriptOutputType.INTEGER, new String[] {token.key()}, token.token());
        return code == null ? 0 : code;
    }
}
