package com.thinking.tennis.app;

import com.thinking.tennis.port.CourtAvailabilityPort;
import com.thinking.tennis.port.CourtAvailabilityPort.CheckFailed;
import com.thinking.tennis.port.CourtAvailabilityPort.CourtAvailabilityCheck;
import com.thinking.tennis.port.CourtAvailabilityPort.CourtNotSupportedException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionException;
import org.springframework.stereotype.Service;

@Service
public class AvailabilityService {

    public static final Duration CHECK_INTERVAL = Duration.ofSeconds(20);

    private final CourtAvailabilityPort availabilityPort;
    private final Map<AvailabilityKey, CourtAvailabilityCheck> successfulChecks = new ConcurrentHashMap<>();
    private final Map<AvailabilityKey, CompletableFuture<CachedResult>> inFlight = new ConcurrentHashMap<>();

    public AvailabilityService(CourtAvailabilityPort availabilityPort) {
        this.availabilityPort = availabilityPort;
    }

    public CachedResult getAvailability(String courtId, LocalDate date) {
        AvailabilityKey key = new AvailabilityKey(courtId, date);
        CourtAvailabilityCheck cached = successfulChecks.get(key);
        if (cached != null && cached.confirmedAt().plus(CHECK_INTERVAL).isAfter(Instant.now())) {
            return CachedResult.fresh(cached);
        }

        CompletableFuture<CachedResult> mine = new CompletableFuture<>();
        CompletableFuture<CachedResult> current = inFlight.putIfAbsent(key, mine);
        if (current != null) {
            return joinSharedResult(current);
        }
        try {
            CachedResult result = refresh(key);
            mine.complete(result);
            return result;
        } catch (RuntimeException exception) {
            mine.completeExceptionally(exception);
            throw exception;
        } finally {
            inFlight.remove(key, mine);
        }
    }

    private CachedResult joinSharedResult(CompletableFuture<CachedResult> current) {
        try {
            return current.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }

    private CachedResult refresh(AvailabilityKey key) {
        try {
            CourtAvailabilityCheck checked = availabilityPort.checkAvailability(key.courtId(), key.date());
            successfulChecks.put(key, checked);
            return CachedResult.fresh(checked);
        } catch (CourtNotSupportedException exception) {
            throw ApplicationException.courtNotSupported(
                    "courtId: " + exception.courtId(), org.springframework.http.HttpStatus.NOT_FOUND);
        } catch (CheckFailed exception) {
            CourtAvailabilityCheck lastSuccess = successfulChecks.get(key);
            if (lastSuccess != null) {
                return CachedResult.stale(lastSuccess, exception.reason().name());
            }
            throw ApplicationException.upstream(toErrorCode(exception.reason()));
        }
    }

    private ErrorCode toErrorCode(CourtAvailabilityPort.UpstreamFailureReason reason) {
        return switch (reason) {
            case UPSTREAM_TIMEOUT -> ErrorCode.UPSTREAM_TIMEOUT;
            case UPSTREAM_UNAVAILABLE -> ErrorCode.UPSTREAM_UNAVAILABLE;
            case UPSTREAM_RESPONSE_UNREADABLE -> ErrorCode.UPSTREAM_RESPONSE_UNREADABLE;
        };
    }

    public Optional<CourtAvailabilityCheck> cached(String courtId, LocalDate date) {
        return Optional.ofNullable(successfulChecks.get(new AvailabilityKey(courtId, date)));
    }

    public record CachedResult(CourtAvailabilityCheck check, boolean stale, String staleReason) {
        static CachedResult fresh(CourtAvailabilityCheck check) {
            return new CachedResult(check, false, null);
        }

        static CachedResult stale(CourtAvailabilityCheck check, String staleReason) {
            return new CachedResult(check, true, staleReason);
        }
    }

    private record AvailabilityKey(String courtId, LocalDate date) {
    }
}
