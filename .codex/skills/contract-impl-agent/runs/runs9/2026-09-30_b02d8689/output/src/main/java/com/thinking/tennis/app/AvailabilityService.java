package com.thinking.tennis.app;

import static com.thinking.tennis.app.AppExceptions.ErrorCode.COURT_NOT_SUPPORTED;
import static com.thinking.tennis.app.AppExceptions.ErrorCode.UPSTREAM_RESPONSE_UNREADABLE;
import static com.thinking.tennis.app.AppExceptions.ErrorCode.UPSTREAM_TIMEOUT;
import static com.thinking.tennis.app.AppExceptions.ErrorCode.UPSTREAM_UNAVAILABLE;

import com.thinking.tennis.app.AppExceptions.CourtNotSupported;
import com.thinking.tennis.app.AppExceptions.UpstreamUnavailable;
import com.thinking.tennis.domain.AvailabilitySlot;
import com.thinking.tennis.domain.CourtAvailabilitySnapshot;
import com.thinking.tennis.domain.TimeSlot;
import com.thinking.tennis.port.CourtAvailabilityPort;
import com.thinking.tennis.port.CourtAvailabilityPort.CheckFailed;
import com.thinking.tennis.port.CourtAvailabilityPort.CourtAvailabilityCheck;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AvailabilityService {

    private final CourtAvailabilityPort courtAvailabilityPort;
    private final InMemoryAlertStore alertStore;
    private final Clock clock;
    private final ConcurrentHashMap<AvailabilityKey, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<AvailabilityKey, FailedAttempt> failedAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<AvailabilityKey, Object> locks = new ConcurrentHashMap<>();

    public AvailabilityService(CourtAvailabilityPort courtAvailabilityPort, InMemoryAlertStore alertStore) {
        this.courtAvailabilityPort = courtAvailabilityPort;
        this.alertStore = alertStore;
        this.clock = Clock.systemUTC();
    }

    public CourtAvailabilitySnapshot getAvailability(String courtId, LocalDate date) {
        if (courtAvailabilityPort.findCourt(courtId).isEmpty()) {
            throw new CourtNotSupported(courtId);
        }
        AvailabilityKey key = new AvailabilityKey(courtId, date);
        CacheEntry existing = cache.get(key);
        Instant now = clock.instant();
        if (existing != null && !isRefreshDue(existing, now)) {
            return existing.toSnapshot();
        }
        FailedAttempt failedAttempt = failedAttempts.get(key);
        if (failedAttempt != null && !isRefreshDue(failedAttempt.attemptedAt(), now)) {
            throw toUpstreamException(failedAttempt.reason());
        }

        Object lock = locks.computeIfAbsent(key, ignored -> new Object());
        synchronized (lock) {
            CacheEntry current = cache.get(key);
            Instant lockedNow = clock.instant();
            if (current != null && !isRefreshDue(current, lockedNow)) {
                return current.toSnapshot();
            }
            FailedAttempt lockedFailure = failedAttempts.get(key);
            if (lockedFailure != null && !isRefreshDue(lockedFailure.attemptedAt(), lockedNow)) {
                throw toUpstreamException(lockedFailure.reason());
            }
            try {
                CourtAvailabilityCheck check = courtAvailabilityPort.checkAvailability(courtId, date);
                CacheEntry fresh = CacheEntry.fresh(check);
                cache.put(key, fresh);
                failedAttempts.remove(key);
                applyAvailabilityToAlerts(fresh);
                return fresh.toSnapshot();
            } catch (CourtAvailabilityPort.CourtNotSupportedException ex) {
                throw new CourtNotSupported(ex.courtId());
            } catch (CheckFailed ex) {
                if (current != null) {
                    CacheEntry stale = current.withStaleReason(ex.reason().name());
                    cache.put(key, stale);
                    return stale.toSnapshot();
                }
                failedAttempts.put(key, new FailedAttempt(ex.reason(), clock.instant()));
                throw toUpstreamException(ex);
            }
        }
    }

    public CourtAvailabilityPort.SupportedCourt findCourt(String courtId) {
        return courtAvailabilityPort.findCourt(courtId).orElseThrow(() -> new CourtNotSupported(courtId));
    }

    public boolean isSlotSupported(String courtId, LocalDate date, TimeSlot slot) {
        CourtAvailabilitySnapshot snapshot = getAvailability(courtId, date);
        return snapshot.slots().stream().anyMatch(item -> item.slot().equals(slot));
    }

    private boolean isRefreshDue(CacheEntry entry, Instant now) {
        return !entry.refreshAt().plus(TennisAlertConstants.AVAILABILITY_CHECK_INTERVAL).isAfter(now);
    }

    private boolean isRefreshDue(Instant lastAttemptAt, Instant now) {
        return !lastAttemptAt.plus(TennisAlertConstants.AVAILABILITY_CHECK_INTERVAL).isAfter(now);
    }

    private void applyAvailabilityToAlerts(CacheEntry fresh) {
        alertStore.expireDue(clock.instant());
        for (var alert : alertStore.listWatchingByCourtDate(fresh.courtId(), fresh.date())) {
            alert.markChecked(fresh.confirmedAt());
            boolean available = fresh.slots().stream()
                    .anyMatch(slot -> slot.slot().equals(alert.slot()) && slot.available());
            if (available) {
                alert.queueDelivery();
            } else {
                alert.clearPendingDelivery();
            }
        }
    }

    private UpstreamUnavailable toUpstreamException(CheckFailed ex) {
        return switch (ex.reason()) {
            case UPSTREAM_TIMEOUT -> new UpstreamUnavailable(UPSTREAM_TIMEOUT, "예약처가 제때 응답하지 않았습니다. 빈자리가 없다는 뜻은 아닙니다.");
            case UPSTREAM_UNAVAILABLE -> new UpstreamUnavailable(UPSTREAM_UNAVAILABLE, "예약처에 연결하지 못했습니다. 빈자리가 없다는 뜻은 아닙니다.");
            case UPSTREAM_RESPONSE_UNREADABLE -> new UpstreamUnavailable(UPSTREAM_RESPONSE_UNREADABLE, "예약처 응답을 읽지 못했습니다. 빈자리가 없다는 뜻은 아닙니다.");
        };
    }

    private record AvailabilityKey(String courtId, LocalDate date) {
    }

    private record FailedAttempt(CourtAvailabilityPort.UpstreamFailureReason reason, Instant attemptedAt) {
    }

    private record CacheEntry(String courtId,
                              String courtName,
                              String reservationUrl,
                              LocalDate date,
                              Instant confirmedAt,
                              Instant lastAttemptAt,
                              String staleReason,
                              List<AvailabilitySlot> slots) {

        static CacheEntry fresh(CourtAvailabilityCheck check) {
            List<AvailabilitySlot> slots = check.slots().stream()
                    .map(slot -> new AvailabilitySlot(new TimeSlot(slot.startTime(), slot.endTime()), slot.available()))
                    .toList();
            return new CacheEntry(
                    check.court().courtId(),
                    check.court().courtName(),
                    check.court().reservationUrl(),
                    check.date(),
                    check.confirmedAt(),
                    Instant.now(),
                    null,
                    slots);
        }

        CacheEntry withStaleReason(String reason) {
            return new CacheEntry(courtId, courtName, reservationUrl, date, confirmedAt, Instant.now(), reason, slots);
        }

        Instant refreshAt() {
            return staleReason == null ? confirmedAt : lastAttemptAt;
        }

        CourtAvailabilitySnapshot toSnapshot() {
            return new CourtAvailabilitySnapshot(courtId, courtName, date, confirmedAt, staleReason != null, staleReason, reservationUrl, slots);
        }
    }
}
