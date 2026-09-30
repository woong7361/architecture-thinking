package com.thinking.tennis.app;

import com.thinking.tennis.domain.AvailabilitySnapshot;
import com.thinking.tennis.port.CourtAvailabilityPort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AvailabilityApplicationService {
    private static final Duration REFRESH_INTERVAL = Duration.ofSeconds(20);

    private final CourtAvailabilityPort availabilityPort;
    private final ConcurrentHashMap<CacheKey, AvailabilitySnapshot> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<CacheKey, CompletableFuture<AvailabilitySnapshot>> inFlight = new ConcurrentHashMap<>();

    public AvailabilityApplicationService(CourtAvailabilityPort availabilityPort) {
        this.availabilityPort = availabilityPort;
    }

    public AvailabilitySnapshot get(String courtId, LocalDate date) {
        return getInternal(courtId, date, true);
    }

    public AvailabilitySnapshot getForAlert(String courtId, LocalDate date) {
        return getInternal(courtId, date, false);
    }

    private AvailabilitySnapshot getInternal(String courtId, LocalDate date, boolean pathLookup) {
        CourtAvailabilityPort.SupportedCourt court = availabilityPort.findCourt(courtId)
                .orElseThrow(() -> pathLookup
                        ? ApiFailure.courtNotSupportedPath(courtId)
                        : ApiFailure.courtNotSupported(courtId));
        CacheKey key = new CacheKey(courtId, date);
        AvailabilitySnapshot cached = cache.get(key);
        if (cached != null && isFresh(cached)) {
            return cached;
        }

        CompletableFuture<AvailabilitySnapshot> leader = new CompletableFuture<>();
        CompletableFuture<AvailabilitySnapshot> shared = inFlight.putIfAbsent(key, leader);
        if (shared == null) {
            shared = leader;
            refresh(key, court, leader, pathLookup);
        }

        try {
            return shared.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof AvailabilityFailure availabilityFailure) {
                throw availabilityFailure;
            }
            if (cause instanceof ApiFailure apiFailure) {
                throw apiFailure;
            }
            throw ApiFailure.internal();
        }
    }

    private void refresh(
            CacheKey key,
            CourtAvailabilityPort.SupportedCourt court,
            CompletableFuture<AvailabilitySnapshot> result,
            boolean pathLookup
    ) {
        try {
            CourtAvailabilityPort.CourtAvailabilityCheck check =
                    availabilityPort.checkAvailability(key.courtId(), key.date());
            AvailabilitySnapshot snapshot = new AvailabilitySnapshot(
                    court,
                    key.date(),
                    check.confirmedAt(),
                    false,
                    null,
                    List.copyOf(check.slots()));
            cache.put(key, snapshot);
            result.complete(snapshot);
        } catch (CourtAvailabilityPort.CheckFailed failure) {
            AvailabilitySnapshot previous = cache.get(key);
            if (previous != null) {
                result.complete(new AvailabilitySnapshot(
                        previous.court(),
                        previous.date(),
                        previous.confirmedAt(),
                        true,
                        failure.reason(),
                        previous.slots()));
            } else {
                result.completeExceptionally(new AvailabilityFailure(failure.reason()));
            }
        } catch (CourtAvailabilityPort.CourtNotSupportedException failure) {
            result.completeExceptionally(pathLookupFailure(key.courtId(), pathLookup));
        } catch (RuntimeException failure) {
            result.completeExceptionally(failure);
        } finally {
            inFlight.remove(key, result);
        }
    }

    private ApiFailure pathLookupFailure(String courtId, boolean pathLookup) {
        return pathLookup ? ApiFailure.courtNotSupportedPath(courtId) : ApiFailure.courtNotSupported(courtId);
    }

    private boolean isFresh(AvailabilitySnapshot snapshot) {
        return Duration.between(snapshot.confirmedAt(), Instant.now()).compareTo(REFRESH_INTERVAL) < 0;
    }

    private record CacheKey(String courtId, LocalDate date) {
    }
}
