package com.thinking.tennis.app;

import com.thinking.tennis.domain.Availability;
import com.thinking.tennis.port.CourtAvailabilityPort;
import com.thinking.tennis.port.CourtAvailabilityPort.CheckFailed;
import com.thinking.tennis.port.CourtAvailabilityPort.CourtAvailabilityCheck;
import com.thinking.tennis.port.CourtAvailabilityPort.CourtNotSupportedException;
import com.thinking.tennis.port.CourtAvailabilityPort.SlotAvailability;
import com.thinking.tennis.port.CourtAvailabilityPort.SupportedCourt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AvailabilityService {
    private static final Duration CHECK_INTERVAL = Duration.ofSeconds(20);

    private final CourtAvailabilityPort courtAvailabilityPort;
    private final ConcurrentHashMap<String, CachedAvailability> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public AvailabilityService(CourtAvailabilityPort courtAvailabilityPort) {
        this.courtAvailabilityPort = courtAvailabilityPort;
    }

    public AvailabilityResult get(String courtId, LocalDate date) {
        String key = key(courtId, date);
        synchronized (locks.computeIfAbsent(key, ignored -> new Object())) {
            Instant now = Instant.now();
            CachedAvailability cached = cache.get(key);
            if (cached != null && isFresh(cached.availability().confirmedAt(), now)) {
                return new AvailabilityResult(cached.availability(), false, null);
            }
            if (cached != null && cached.failureAt() != null
                    && isFresh(cached.failureAt(), now)) {
                if (cached.availability() != null) {
                    return new AvailabilityResult(cached.availability(), true, cached.failureReason());
                }
                throw upstreamFailure(cached.failureReason());
            }

            Optional<SupportedCourt> supportedCourt = courtAvailabilityPort.findCourt(courtId);
            if (supportedCourt.isEmpty()) {
                throw new UseCaseException(
                        FailureCode.COURT_NOT_SUPPORTED,
                        FailureContext.AVAILABILITY_PATH,
                        "courtId: " + courtId,
                        false,
                        null
                );
            }

            try {
                CourtAvailabilityCheck checked = courtAvailabilityPort.checkAvailability(courtId, date);
                Availability availability = toDomain(checked);
                cache.put(key, CachedAvailability.success(availability));
                return new AvailabilityResult(availability, false, null);
            } catch (CourtNotSupportedException exception) {
                throw new UseCaseException(
                        FailureCode.COURT_NOT_SUPPORTED,
                        FailureContext.AVAILABILITY_PATH,
                        "courtId: " + exception.courtId(),
                        false,
                        null
                );
            } catch (CheckFailed exception) {
                CachedAvailability failed = cached == null
                        ? CachedAvailability.failure(now, exception.reason())
                        : cached.withFailure(now, exception.reason());
                cache.put(key, failed);
                if (cached != null && cached.availability() != null) {
                    return new AvailabilityResult(cached.availability(), true, exception.reason());
                }
                throw upstreamFailure(exception.reason());
            }
        }
    }

    private Availability toDomain(CourtAvailabilityCheck checked) {
        List<Availability.Slot> slots = checked.slots().stream()
                .map(this::toDomain)
                .toList();
        return new Availability(
                checked.court().courtId(),
                checked.court().courtName(),
                checked.court().reservationUrl(),
                checked.date(),
                checked.confirmedAt(),
                slots
        );
    }

    private Availability.Slot toDomain(SlotAvailability slot) {
        return new Availability.Slot(slot.startTime(), slot.endTime(), slot.available());
    }

    private UseCaseException upstreamFailure(
            com.thinking.tennis.port.CourtAvailabilityPort.UpstreamFailureReason reason
    ) {
        FailureCode code = switch (reason) {
            case UPSTREAM_TIMEOUT -> FailureCode.UPSTREAM_TIMEOUT;
            case UPSTREAM_UNAVAILABLE -> FailureCode.UPSTREAM_UNAVAILABLE;
            case UPSTREAM_RESPONSE_UNREADABLE -> FailureCode.UPSTREAM_RESPONSE_UNREADABLE;
        };
        return new UseCaseException(code, FailureContext.AVAILABILITY_PATH,
                "예약 상태를 확인하지 못했습니다.", true, 20);
    }

    private boolean isFresh(Instant confirmedAt, Instant now) {
        return confirmedAt != null && !confirmedAt.plus(CHECK_INTERVAL).isBefore(now);
    }

    private String key(String courtId, LocalDate date) {
        return courtId + "|" + date;
    }

    private record CachedAvailability(
            Availability availability,
            Instant failureAt,
            com.thinking.tennis.port.CourtAvailabilityPort.UpstreamFailureReason failureReason
    ) {
        static CachedAvailability success(Availability availability) {
            return new CachedAvailability(availability, null, null);
        }

        static CachedAvailability failure(
                Instant failureAt,
                com.thinking.tennis.port.CourtAvailabilityPort.UpstreamFailureReason reason
        ) {
            return new CachedAvailability(null, failureAt, reason);
        }

        CachedAvailability withFailure(
                Instant failureAt,
                com.thinking.tennis.port.CourtAvailabilityPort.UpstreamFailureReason reason
        ) {
            return new CachedAvailability(availability, failureAt, reason);
        }
    }
}
