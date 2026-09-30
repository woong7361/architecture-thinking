package com.thinking.tennis.api;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 멱등 키와 그 키로 끝난 응답을 들고 있다.
 *
 * <p>보관하는 것은 2xx로 끝난 응답뿐이다. 4xx는 요청을 고쳐야 풀리므로 기록하지 않고, 같은 키로 다시
 * 보내면 조건을 다시 판정한다. 5xx도 기록하지 않아 같은 키로 그대로 다시 보내면 다시 처리된다.
 *
 * <p>보관하는 것이 만들어진 신청이 아니라 <b>돌려준 응답</b>인 이유는 재생되는 응답이 처음 처리한 시점의
 * 내용이어야 하기 때문이다. 신청만 들고 있다가 재생할 때 다시 그려 내면 그 뒤로 바뀐 상태와 그때 계산되는
 * 확인 시각이 섞여 들어가 "그 뒤로 상태가 바뀌었어도 재생 응답은 바뀌지 않는다"는 약속이 깨진다.
 *
 * <p>키는 사용자와 오퍼레이션 단위다. 다른 사용자가 우연히 같은 키를 골라도 서로의 응답을 받지 않는다.
 */
@Component
public class IdempotencyRegistry {

    /** 보관 기간이다. 이 기간이 지난 뒤 같은 키로 다시 보내면 재생이 아니라 새 요청으로 처리한다. */
    private static final Duration RETENTION = Duration.ofHours(24);

    /**
     * 같은 키로 앞서 돌려준 응답이다.
     *
     * @param status   그때의 상태 코드
     * @param body     그때의 본문
     * @param location 그때의 {@code Location} 헤더 값
     */
    public record Replay(int status, Object body, String location) {
    }

    /** {@code replay} 가 null이면 그 키의 요청이 아직 처리 중이다. */
    private record Entry(String fingerprint, Instant storedAt, Replay replay) {
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Clock clock;

    public IdempotencyRegistry(Clock clock) {
        this.clock = clock;
    }

    /**
     * 이 키의 처리를 시작하거나 앞선 응답을 돌려준다.
     *
     * @return 앞서 2xx로 끝난 응답이 있으면 그 응답, 없으면 빈 값. 빈 값을 받은 쪽은 처리를 끝내고
     *         반드시 {@link #complete} 나 {@link #abandon} 을 불러 처리 중 표시를 걷어야 한다
     * @throws ApiFailure.IdempotencyKeyReused 같은 키가 다른 내용의 요청에 이미 쓰였다
     * @throws ApiFailure.RequestInProgress    같은 키의 앞선 요청이 아직 처리 중이다
     */
    public Optional<Replay> beginOrReplay(String userId, String operationId, String key, String fingerprint) {
        String id = id(userId, operationId, key);
        while (true) {
            Instant now = clock.instant();
            Entry started = new Entry(fingerprint, now, null);
            Entry existing = entries.putIfAbsent(id, started);
            if (existing == null) {
                return Optional.empty();
            }
            if (expired(existing, now)) {
                /* 보관 기간이 지난 기록은 없는 것과 같다. 자리를 빼앗지 못하면 다시 본다. */
                if (entries.replace(id, existing, started)) {
                    return Optional.empty();
                }
                continue;
            }
            if (!existing.fingerprint().equals(fingerprint)) {
                throw new ApiFailure.IdempotencyKeyReused();
            }
            if (existing.replay() == null) {
                throw new ApiFailure.RequestInProgress();
            }
            return Optional.of(existing.replay());
        }
    }

    /** 2xx로 끝난 응답을 보관한다. */
    public void complete(String userId, String operationId, String key, String fingerprint, Replay replay) {
        entries.put(id(userId, operationId, key), new Entry(fingerprint, clock.instant(), replay));
    }

    /** 2xx로 끝나지 않은 요청의 처리 중 표시를 걷는다. */
    public void abandon(String userId, String operationId, String key) {
        entries.remove(id(userId, operationId, key));
    }

    private boolean expired(Entry entry, Instant now) {
        return now.isAfter(entry.storedAt().plus(RETENTION));
    }

    private static String id(String userId, String operationId, String key) {
        return userId + "|" + operationId + "|" + key;
    }
}
