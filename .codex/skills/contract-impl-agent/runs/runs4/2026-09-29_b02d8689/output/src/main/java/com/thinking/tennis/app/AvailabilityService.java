package com.thinking.tennis.app;

import com.thinking.tennis.domain.AvailabilitySnapshot;
import com.thinking.tennis.port.CourtAvailabilityPort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AvailabilityService {

    private static final Duration REFRESH_INTERVAL = Duration.ofSeconds(20);
    private static final int RETRY_AFTER_SECONDS = 20;
    private final CourtAvailabilityPort courtAvailabilityPort;
    private final Map<AvailabilityKey, AvailabilitySnapshot> snapshots = new ConcurrentHashMap<>();
    private final Map<AvailabilityKey, Object> locks = new ConcurrentHashMap<>();

    public AvailabilityService(CourtAvailabilityPort courtAvailabilityPort) {
        this.courtAvailabilityPort = courtAvailabilityPort;
    }

    public AvailabilitySnapshot get(LocalDate date, String courtId, Instant now) {
        AvailabilityKey key = new AvailabilityKey(courtId, date);
        AvailabilitySnapshot current = snapshots.get(key);
        if (current != null && isFresh(current, now)) {
            return current;
        }
        synchronized (locks.computeIfAbsent(key, ignored -> new Object())) {
            current = snapshots.get(key);
            if (current != null && isFresh(current, now)) {
                return current;
            }
            return refresh(key, now, current);
        }
    }

    public CourtAvailabilityPort.SupportedCourt requireCourt(String courtId) {
        return courtAvailabilityPort.findCourt(courtId)
                .orElseThrow(() -> new UseCaseException(
                        UseCaseException.Code.COURT_NOT_SUPPORTED,
                        "지원하지 않는 코트입니다: " + courtId));
    }

    public void requireSlot(AvailabilitySnapshot snapshot, LocalTime startTime, LocalTime endTime) {
        boolean supported = snapshot.slots().stream()
                .anyMatch(slot -> slot.startTime().equals(startTime) && slot.endTime().equals(endTime));
        if (!supported) {
            throw new UseCaseException(
                    UseCaseException.Code.SLOT_NOT_SUPPORTED,
                    "지원하지 않는 시간대입니다");
        }
    }

    private AvailabilitySnapshot refresh(
            AvailabilityKey key,
            Instant now,
            AvailabilitySnapshot previous) {
        try {
            CourtAvailabilityPort.CourtAvailabilityCheck result =
                    courtAvailabilityPort.checkAvailability(key.courtId(), key.date());
            AvailabilitySnapshot fresh = new AvailabilitySnapshot(
                    result.court(), result.date(), result.confirmedAt(), result.slots(), false, null);
            snapshots.put(key, fresh);
            return fresh;
        } catch (CourtAvailabilityPort.CourtNotSupportedException exception) {
            throw new UseCaseException(
                    UseCaseException.Code.COURT_NOT_SUPPORTED,
                    "지원하지 않는 코트입니다: " + exception.courtId());
        } catch (CourtAvailabilityPort.CheckFailed exception) {
            if (previous != null) {
                return new AvailabilitySnapshot(
                        previous.court(),
                        previous.date(),
                        previous.confirmedAt(),
                        previous.slots(),
                        true,
                        exception.reason());
            }
            throw upstreamFailure(exception.reason());
        }
    }

    private UseCaseException upstreamFailure(CourtAvailabilityPort.UpstreamFailureReason reason) {
        return switch (reason) {
            case UPSTREAM_TIMEOUT -> new UseCaseException(
                    UseCaseException.Code.UPSTREAM_TIMEOUT,
                    "예약처가 제때 응답하지 않았습니다",
                    RETRY_AFTER_SECONDS);
            case UPSTREAM_UNAVAILABLE -> new UseCaseException(
                    UseCaseException.Code.UPSTREAM_UNAVAILABLE,
                    "예약처에 연결하지 못했습니다",
                    RETRY_AFTER_SECONDS);
            case UPSTREAM_RESPONSE_UNREADABLE -> new UseCaseException(
                    UseCaseException.Code.UPSTREAM_RESPONSE_UNREADABLE,
                    "예약처 응답을 읽지 못했습니다",
                    RETRY_AFTER_SECONDS);
        };
    }

    private boolean isFresh(AvailabilitySnapshot snapshot, Instant now) {
        return Duration.between(snapshot.confirmedAt(), now).compareTo(REFRESH_INTERVAL) <= 0;
    }

    private record AvailabilityKey(String courtId, LocalDate date) {
    }
}
