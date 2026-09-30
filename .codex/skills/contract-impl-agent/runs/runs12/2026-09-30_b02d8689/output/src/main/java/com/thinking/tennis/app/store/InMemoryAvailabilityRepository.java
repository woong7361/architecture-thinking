package com.thinking.tennis.app.store;

import com.thinking.tennis.app.AvailabilityRepository;
import com.thinking.tennis.domain.CourtDayAvailability;
import com.thinking.tennis.domain.UpstreamFailureReason;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link AvailabilityRepository} 의 인메모리 구현이다.
 *
 * <p>성공과 실패를 다른 표에 담는다. 같은 표에 담으면 확인 실패가 마지막 성공 결과를 덮어써 확인 실패와
 * 빈자리 없음이 같은 모양이 된다.
 */
@Component
public class InMemoryAvailabilityRepository implements AvailabilityRepository {

    private final Map<String, CourtDayAvailability> successes = new ConcurrentHashMap<>();
    private final Map<String, FailedCheck> failures = new ConcurrentHashMap<>();

    @Override
    public Optional<CourtDayAvailability> findLastSuccess(String courtId, LocalDate date) {
        return Optional.ofNullable(successes.get(key(courtId, date)));
    }

    @Override
    public void storeSuccess(CourtDayAvailability availability) {
        successes.put(key(availability.courtId(), availability.date()), availability);
    }

    @Override
    public Optional<FailedCheck> findLastFailure(String courtId, LocalDate date) {
        return Optional.ofNullable(failures.get(key(courtId, date)));
    }

    @Override
    public void storeFailure(String courtId, LocalDate date, UpstreamFailureReason reason, Instant failedAt) {
        failures.put(key(courtId, date), new FailedCheck(reason, failedAt));
    }

    @Override
    public void clearFailure(String courtId, LocalDate date) {
        failures.remove(key(courtId, date));
    }

    private static String key(String courtId, LocalDate date) {
        return courtId + "@" + date;
    }
}
