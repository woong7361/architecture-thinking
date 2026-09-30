package com.thinking.tennis.app.store;

import com.thinking.tennis.app.AlertView;
import com.thinking.tennis.app.IdempotencyStore;
import com.thinking.tennis.app.WatchPolicy;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link IdempotencyStore} 의 인메모리 구현이다.
 *
 * <p>자리를 잡는 일을 {@code putIfAbsent} 한 번으로 하는 이유는 같은 키로 동시에 들어온 두 요청 중 하나만
 * 처리를 시작해야 하기 때문이다. 조회하고 나서 저장하면 그 사이에 둘 다 빈자리를 보고 둘 다 처리를
 * 시작한다.
 */
@Component
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final Map<String, Slot> slots = new ConcurrentHashMap<>();

    @Override
    public Claim claim(String userId, String operationId, String key, String fingerprint, Instant now) {
        String storageKey = key(userId, operationId, key);
        while (true) {
            Slot fresh = new Slot(fingerprint, now);
            Slot existing = slots.putIfAbsent(storageKey, fresh);
            if (existing == null) {
                return Claim.started();
            }
            if (isRetentionPassed(existing, now)) {
                /* 보관 기간이 지난 키는 재생이 아니라 새 요청으로 처리한다. */
                slots.remove(storageKey, existing);
                continue;
            }
            if (!existing.completed) {
                /*
                 * 같은 키의 앞선 요청이 아직 처리 중이다. 내용이 다르더라도 먼저 물러난다. 앞선 요청이
                 * 4xx로 끝나면 그 키는 쓰이지 않은 것이 되므로, 아직 쓰였다고 말하는 409를 낼 수 없다.
                 * 다만 내용이 다르다는 사실은 자리를 잡을 때 이미 적혀 있으므로 안내를 가른다.
                 */
                return Claim.inProgress(existing.fingerprint.equals(fingerprint));
            }
            if (!existing.fingerprint.equals(fingerprint)) {
                return Claim.fingerprintMismatch();
            }
            return Claim.replay(existing.statusCode, existing.view);
        }
    }

    @Override
    public void complete(String userId, String operationId, String key, int statusCode, AlertView view, Instant now) {
        Slot slot = slots.get(key(userId, operationId, key));
        if (slot == null) {
            return;
        }
        slot.statusCode = statusCode;
        slot.view = view;
        slot.recordedAt = now;
        slot.completed = true;
    }

    @Override
    public void release(String userId, String operationId, String key) {
        String storageKey = key(userId, operationId, key);
        Slot slot = slots.get(storageKey);
        if (slot != null && !slot.completed) {
            slots.remove(storageKey, slot);
        }
    }

    private static boolean isRetentionPassed(Slot slot, Instant now) {
        Instant from = slot.recordedAt != null ? slot.recordedAt : slot.claimedAt;
        return now.isAfter(from.plus(WatchPolicy.IDEMPOTENCY_RETENTION));
    }

    private static String key(String userId, String operationId, String key) {
        return userId + "|" + operationId + "|" + key;
    }

    /**
     * 멱등 키 하나가 잡은 자리다.
     *
     * <p>처리 중인 자리와 응답이 채워진 자리를 한 타입으로 둔다. 같은 키의 다음 요청이 봐야 하는 것은
     * 그 둘 중 어느 쪽인지이므로, 두 표로 나누면 두 표 사이의 어긋남을 다뤄야 한다.
     */
    private static final class Slot {

        private final String fingerprint;
        private final Instant claimedAt;
        private volatile boolean completed;
        private volatile int statusCode;
        private volatile AlertView view;
        private volatile Instant recordedAt;

        private Slot(String fingerprint, Instant claimedAt) {
            this.fingerprint = fingerprint;
            this.claimedAt = claimedAt;
        }
    }
}
