package com.thinking.tennis.kata;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

// 강제 종료 실험에서 쓰는 별도 프로세스다. 락을 잡은 뒤 해제하지 않고 기다리며, 부모가 이 프로세스를 강제 종료한다.
public final class ZombieLockHolder {
    private ZombieLockHolder() {
    }

    public static void main(String[] args) throws Exception {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        long leaseMillis = Long.parseLong(args[2]);
        String key = args[3];

        RedisClient client = RedisClient.create(RedisURI.Builder.redis(host, port).build());
        StatefulRedisConnection<String, String> connection = client.connect();
        RedisSpinLock lock = new RedisSpinLock(
                connection.sync(), Duration.ofMillis(leaseMillis), Duration.ofMillis(10), ReleaseMode.TOKEN_CHECKED);

        LockToken token = lock.tryAcquire(key).orElseThrow(() -> new IllegalStateException("Lock already held"));
        System.out.println("ACQUIRED " + System.currentTimeMillis() + " " + token.token());
        System.out.flush();

        TimeUnit.HOURS.sleep(1);
    }
}
