package com.thinking.tennis.app;

import com.thinking.tennis.domain.AvailabilityCacheEntry;
import com.thinking.tennis.domain.AvailabilitySnapshot;
import com.thinking.tennis.port.CourtAvailabilityPort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AvailabilityService {
    public static final long FRESHNESS_SECONDS = 20L;

    private final CourtAvailabilityPort courtAvailabilityPort;
    private final ConcurrentMap<String, AvailabilityCacheEntry> cache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CompletableFuture<AvailabilityResult>> inFlight = new ConcurrentHashMap<>();

    public AvailabilityService(CourtAvailabilityPort courtAvailabilityPort) {
        this.courtAvailabilityPort = courtAvailabilityPort;
    }

    public AvailabilityResult get(String courtId, LocalDate date) {
        String key = key(courtId, date);
        AvailabilityCacheEntry current = cache.get(key);
        if (current != null && current.hasFreshSnapshot(Instant.now(), FRESHNESS_SECONDS)) {
            return new AvailabilityResult(current.snapshot(), false, null);
        }

        CompletableFuture<AvailabilityResult> refresh = new CompletableFuture<>();
        CompletableFuture<AvailabilityResult> existing = inFlight.putIfAbsent(key, refresh);
        if (existing != null) {
            return await(existing);
        }

        try {
            AvailabilityResult result = refresh(courtId, date, current);
            refresh.complete(result);
            return result;
        } catch (RuntimeException exception) {
            refresh.completeExceptionally(exception);
            throw exception;
        } finally {
            inFlight.remove(key, refresh);
        }
    }

    public Optional<Boolean> cachedSlotSupport(
            String courtId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {
        AvailabilityCacheEntry entry = cache.get(key(courtId, date));
        if (entry == null || entry.snapshot() == null) {
            return Optional.empty();
        }
        return Optional.of(entry.snapshot().slots().stream().anyMatch(slot ->
                slot.startTime().equals(startTime)
                        && slot.endTime().equals(endTime)
        ));
    }

    public Optional<CourtAvailabilityPort.SupportedCourt> supportedCourt(String courtId) {
        return courtAvailabilityPort.findCourt(courtId);
    }

    private AvailabilityResult refresh(
            String courtId,
            LocalDate date,
            AvailabilityCacheEntry previous
    ) {
        try {
            CourtAvailabilityPort.SupportedCourt court = courtAvailabilityPort.findCourt(courtId)
                    .orElseThrow(() -> failure(
                            FailureCode.COURT_NOT_SUPPORTED,
                            404,
                            false,
                            null,
                            "지원하지 않는 코트입니다"
                    ));
            CourtAvailabilityPort.CourtAvailabilityCheck checked =
                    courtAvailabilityPort.checkAvailability(courtId, date);
            AvailabilitySnapshot snapshot = new AvailabilitySnapshot(
                    court,
                    date,
                    checked.confirmedAt(),
                    checked.slots()
            );
            cache.put(key(courtId, date), AvailabilityCacheEntry.success(snapshot));
            return new AvailabilityResult(snapshot, false, null);
        } catch (CourtAvailabilityPort.CourtNotSupportedException exception) {
            throw failure(
                    FailureCode.COURT_NOT_SUPPORTED,
                    404,
                    false,
                    null,
                    "지원하지 않는 코트입니다"
            );
        } catch (CourtAvailabilityPort.CheckFailed exception) {
            Instant failedAt = Instant.now();
            AvailabilityCacheEntry failed = AvailabilityCacheEntry.failure(
                    previous == null ? null : previous.snapshot(),
                    failedAt,
                    exception.reason(),
                    null
            );
            cache.put(key(courtId, date), failed);
            if (failed.snapshot() != null) {
                return new AvailabilityResult(failed.snapshot(), true, failed.failureReason());
            }
            throw upstreamFailure(exception.reason());
        }
    }

    private AvailabilityResult await(CompletableFuture<AvailabilityResult> future) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }

    private ApplicationFailure upstreamFailure(CourtAvailabilityPort.UpstreamFailureReason reason) {
        FailureCode code = switch (reason) {
            case UPSTREAM_TIMEOUT -> FailureCode.UPSTREAM_TIMEOUT;
            case UPSTREAM_UNAVAILABLE -> FailureCode.UPSTREAM_UNAVAILABLE;
            case UPSTREAM_RESPONSE_UNREADABLE -> FailureCode.UPSTREAM_RESPONSE_UNREADABLE;
        };
        int status = switch (reason) {
            case UPSTREAM_TIMEOUT -> 504;
            case UPSTREAM_UNAVAILABLE -> 503;
            case UPSTREAM_RESPONSE_UNREADABLE -> 502;
        };
        return failure(code, status, true, (int) FRESHNESS_SECONDS, "예약 상태를 확인하지 못했습니다");
    }

    private ApplicationFailure failure(
            FailureCode code,
            int status,
            boolean retryable,
            Integer retryAfterSeconds,
            String message
    ) {
        return ApplicationFailure.of(code, status, retryable, retryAfterSeconds, message);
    }

    private String key(String courtId, LocalDate date) {
        return courtId + "|" + date;
    }
}
