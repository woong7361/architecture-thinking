package com.thinking.tennis.app;

import com.thinking.tennis.domain.AlertRepository;
import com.thinking.tennis.domain.AvailabilitySnapshot;
import com.thinking.tennis.port.CourtAvailabilityPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AvailabilityService {

    public static final Duration CHECK_INTERVAL = Duration.ofSeconds(20);

    private final CourtAvailabilityPort availabilityPort;
    private final AlertRepository alertRepository;
    private final Clock clock;
    private final ConcurrentMap<Key, AvailabilitySnapshot> lastSuccessful = new ConcurrentHashMap<>();
    private final ConcurrentMap<Key, FailureRecord> lastFailures = new ConcurrentHashMap<>();
    private final ConcurrentMap<Key, Object> locks = new ConcurrentHashMap<>();

    public AvailabilityService(CourtAvailabilityPort availabilityPort, AlertRepository alertRepository) {
        this(availabilityPort, alertRepository, Clock.systemUTC());
    }

    AvailabilityService(CourtAvailabilityPort availabilityPort, AlertRepository alertRepository, Clock clock) {
        this.availabilityPort = availabilityPort;
        this.alertRepository = alertRepository;
        this.clock = clock;
    }

    public Optional<CourtAvailabilityPort.SupportedCourt> findCourt(String courtId) {
        return availabilityPort.findCourt(courtId);
    }

    public AvailabilitySnapshot getCourtAvailability(String courtId, LocalDate date) {
        if (availabilityPort.findCourt(courtId).isEmpty()) {
            throw new CourtNotSupported(courtId);
        }

        Key key = new Key(courtId, date);
        AvailabilitySnapshot cached = lastSuccessful.get(key);
        if (cached != null && !isOlderThanInterval(cached)) {
            return cached;
        }
        FailureRecord recentFailure = lastFailures.get(key);
        if (recentFailure != null && !isOlderThanInterval(recentFailure.failedAt())) {
            return staleOrThrow(cached, recentFailure.reason());
        }

        Object lock = locks.computeIfAbsent(key, ignored -> new Object());
        synchronized (lock) {
            AvailabilitySnapshot afterWait = lastSuccessful.get(key);
            if (afterWait != null && !isOlderThanInterval(afterWait)) {
                return afterWait;
            }
            FailureRecord failureAfterWait = lastFailures.get(key);
            if (failureAfterWait != null && !isOlderThanInterval(failureAfterWait.failedAt())) {
                return staleOrThrow(afterWait, failureAfterWait.reason());
            }
            try {
                AvailabilitySnapshot fresh = toSnapshot(availabilityPort.checkAvailability(courtId, date));
                lastSuccessful.put(key, fresh);
                lastFailures.remove(key);
                alertRepository.markChecked(courtId, date, fresh.confirmedAt());
                return fresh;
            } catch (CourtAvailabilityPort.CheckFailed failure) {
                lastFailures.put(key, new FailureRecord(failure.reason(), clock.instant()));
                return staleOrThrow(lastSuccessful.get(key), failure.reason());
            } catch (CourtAvailabilityPort.CourtNotSupportedException failure) {
                throw new CourtNotSupported(failure.courtId());
            }
        }
    }

    private AvailabilitySnapshot staleOrThrow(AvailabilitySnapshot last,
                                              CourtAvailabilityPort.UpstreamFailureReason reason) {
        if (last != null) {
            return new AvailabilitySnapshot(last.courtId(), last.courtName(), last.date(), last.confirmedAt(),
                    true, reason, last.reservationUrl(), last.slots());
        }
        throw new UpstreamAvailabilityFailure(reason);
    }

    private boolean isOlderThanInterval(AvailabilitySnapshot snapshot) {
        return Duration.between(snapshot.confirmedAt(), clock.instant()).compareTo(CHECK_INTERVAL) > 0;
    }

    private boolean isOlderThanInterval(java.time.Instant instant) {
        return Duration.between(instant, clock.instant()).compareTo(CHECK_INTERVAL) > 0;
    }

    private AvailabilitySnapshot toSnapshot(CourtAvailabilityPort.CourtAvailabilityCheck check) {
        List<AvailabilitySnapshot.Slot> slots = check.slots().stream()
                .map(slot -> new AvailabilitySnapshot.Slot(slot.startTime(), slot.endTime(), slot.available()))
                .toList();
        return new AvailabilitySnapshot(check.court().courtId(), check.court().courtName(), check.date(),
                check.confirmedAt(), false, null, check.court().reservationUrl(), slots);
    }

    private record Key(String courtId, LocalDate date) {
    }

    private record FailureRecord(CourtAvailabilityPort.UpstreamFailureReason reason,
                                 java.time.Instant failedAt) {
    }

    public static class CourtNotSupported extends RuntimeException {

        private final String courtId;

        public CourtNotSupported(String courtId) {
            this.courtId = courtId;
        }

        public String courtId() {
            return courtId;
        }
    }

    public static class UpstreamAvailabilityFailure extends RuntimeException {

        private final CourtAvailabilityPort.UpstreamFailureReason reason;

        public UpstreamAvailabilityFailure(CourtAvailabilityPort.UpstreamFailureReason reason) {
            this.reason = reason;
        }

        public CourtAvailabilityPort.UpstreamFailureReason reason() {
            return reason;
        }
    }
}
