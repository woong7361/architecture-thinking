package com.thinking.tennis.kata;

import java.time.LocalDate;
import java.util.Comparator;

// 락 한 건이다. 전역 순서는 종류의 순위를 먼저 보고 같으면 키를 비교하며, 동일성도 이 두 값으로 판정한다.
public record LockRequest(LockKind kind, String key) {
    public static final Comparator<LockRequest> GLOBAL_ORDER =
            Comparator.comparing(LockRequest::kind).thenComparing(LockRequest::key);

    public static LockRequest availability(long courtId, LocalDate playDate) {
        return new LockRequest(LockKind.AVAILABILITY, AvailabilityLockKey.of(courtId, playDate));
    }

    public static LockRequest alert(SubscribeCommand command) {
        return new LockRequest(LockKind.ALERT, AlertLockKey.of(command));
    }
}
