package com.thinking.tennis.app;

import com.thinking.tennis.port.CourtAvailabilityPort.CourtAvailabilityCheck;
import com.thinking.tennis.port.CourtAvailabilityPort.UpstreamFailureReason;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 코트·날짜마다 확인 결과를 들고 있는 인메모리 저장소다.
 *
 * <p>마지막 성공 결과와 마지막 시도를 따로 들고 있다. 확인이 실패해도 마지막 성공 결과는 남겨야 계약이
 * 말한 대로 그 결과를 200에 {@code stale} 을 세워 돌려줄 수 있고, 마지막 시도 시각을 따로 들고 있어야
 * 실패한 시도도 확인 간격을 채운 것으로 세어 예약처로 나가는 조회가 간격마다 한 번이 된다.
 * 실패를 세지 않으면 예약처가 죽은 동안 들어오는 요청마다 조회가 나간다.
 */
@Component
public class AvailabilityStore {

    /**
     * 코트·날짜 하나의 확인 기록이다.
     *
     * @param lastSuccess   마지막으로 성공한 확인 결과. 한 번도 성공하지 못했으면 null이다
     * @param lastAttemptAt 마지막으로 확인을 시도한 시각
     * @param lastFailure   마지막 시도가 실패한 이유. 마지막 시도가 성공했으면 null이다
     */
    public record Entry(CourtAvailabilityCheck lastSuccess,
                        Instant lastAttemptAt,
                        UpstreamFailureReason lastFailure) {
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    /** 코트·날짜마다 확인을 한 번으로 묶는 자리다. */
    private final Map<String, Object> checkLocks = new ConcurrentHashMap<>();

    public Optional<Entry> find(String courtId, LocalDate date) {
        return Optional.ofNullable(entries.get(key(courtId, date)));
    }

    public Entry recordSuccess(CourtAvailabilityCheck check, Instant attemptedAt) {
        Entry entry = new Entry(check, attemptedAt, null);
        entries.put(key(check.court().courtId(), check.date()), entry);
        return entry;
    }

    public Entry recordFailure(String courtId, LocalDate date, Instant attemptedAt,
                               UpstreamFailureReason reason) {
        Entry previous = entries.get(key(courtId, date));
        Entry entry = new Entry(previous == null ? null : previous.lastSuccess(), attemptedAt, reason);
        entries.put(key(courtId, date), entry);
        return entry;
    }

    /** 이 코트·날짜를 마지막으로 확인하는 데 성공한 시각이다. 한 번도 성공하지 못했으면 빈 값이다. */
    public Optional<Instant> lastConfirmedAt(String courtId, LocalDate date) {
        return find(courtId, date)
                .map(Entry::lastSuccess)
                .map(CourtAvailabilityCheck::confirmedAt);
    }

    Object checkLock(String courtId, LocalDate date) {
        return checkLocks.computeIfAbsent(key(courtId, date), ignored -> new Object());
    }

    private static String key(String courtId, LocalDate date) {
        return courtId + "@" + date;
    }
}
