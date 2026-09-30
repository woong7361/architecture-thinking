package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.AvailabilitySnapshot;
import com.thinking.tennis.port.CourtAvailabilityPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertApplicationService {
    private static final long AVAILABILITY_REFRESH_SECONDS = 20;
    private static final long CHECK_DELAY_SECONDS = AVAILABILITY_REFRESH_SECONDS * 3;
    private static final long IDEMPOTENCY_RETENTION_SECONDS = 24 * 60 * 60;
    private static final ZoneId COURT_ZONE = ZoneId.of("Asia/Seoul");

    private final CourtAvailabilityPort availabilityPort;
    private final Clock clock;
    private final Map<UUID, Alert> alerts = new ConcurrentHashMap<>();
    private final Map<String, AvailabilitySnapshot> availabilitySnapshots = new ConcurrentHashMap<>();
    private final Map<String, FailureMarker> availabilityFailures = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<AvailabilitySnapshot>> refreshes = new ConcurrentHashMap<>();
    private final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();

    public AlertApplicationService(CourtAvailabilityPort availabilityPort, Clock clock) {
        this.availabilityPort = availabilityPort;
        this.clock = clock;
    }

    public AvailabilitySnapshot getCourtAvailability(String courtId, LocalDate date) {
        Instant now = clock.instant();
        String key = availabilityKey(courtId, date);
        AvailabilitySnapshot cached = availabilitySnapshots.get(key);
        FailureMarker failure = availabilityFailures.get(key);
        if (cached != null && ageSeconds(cached.confirmedAt(), now) < AVAILABILITY_REFRESH_SECONDS
                && failure == null) {
            return cached;
        }
        if (cached == null && failure != null && ageSeconds(failure.failedAt(), now) < AVAILABILITY_REFRESH_SECONDS) {
            throw failure.asException();
        }

        CompletableFuture<AvailabilitySnapshot> future = new CompletableFuture<>();
        CompletableFuture<AvailabilitySnapshot> current = refreshes.putIfAbsent(key, future);
        if (current != null) {
            return awaitRefresh(current);
        }
        try {
            AvailabilitySnapshot result = refreshAvailability(courtId, date, cached);
            future.complete(result);
            return result;
        } catch (RuntimeException exception) {
            future.completeExceptionally(exception);
            throw exception;
        } finally {
            refreshes.remove(key, future);
        }
    }

    public CreateResult createAlert(String ownerId,
                                    String courtId,
                                    LocalDate date,
                                    LocalTime startTime,
                                    LocalTime endTime,
                                    String idempotencyKey) {
        Instant now = clock.instant();
        purgeExpiredIdempotency(now);
        String fingerprint = String.join("|", ownerId, courtId, date.toString(), startTime.toString(), endTime.toString());
        IdempotencyRecord existing = idempotency.get(idempotencyKey);
        if (existing != null) {
            if (!existing.fingerprint().equals(fingerprint)) {
                throw problem(409, "IDEMPOTENCY_KEY_REUSED", "The idempotency key was used for different request content.", false, null);
            }
            if (existing.inFlight()) {
                throw problem(503, "CONCURRENT_UPDATE_CONFLICT", "The same idempotency key is still being processed.", true, 1);
            }
            return new CreateResult(existing.result().alert().copy(), existing.result().statusCode(), true);
        }

        IdempotencyRecord claim = new IdempotencyRecord(fingerprint, now, null, true);
        IdempotencyRecord previous = idempotency.putIfAbsent(idempotencyKey, claim);
        if (previous != null) {
            if (!previous.fingerprint().equals(fingerprint)) {
                throw problem(409, "IDEMPOTENCY_KEY_REUSED", "The idempotency key was used for different request content.", false, null);
            }
            throw problem(503, "CONCURRENT_UPDATE_CONFLICT", "The same idempotency key is still being processed.", true, 1);
        }

        try {
            expireDueAlerts(now);
            CourtAvailabilityPort.SupportedCourt court = availabilityPort.findCourt(courtId)
                    .orElseThrow(() -> problem(422, "COURT_NOT_SUPPORTED", "The requested court is not supported.", false, null));
            AvailabilitySnapshot snapshot = availabilitySnapshots.get(availabilityKey(courtId, date));
            if (snapshot == null) {
                throw problem(422, "SLOT_NOT_SUPPORTED",
                        "The requested slot is not supported by the currently known operating schedule.", false, null);
            }
            boolean slotSupported = snapshot.slots().stream()
                    .anyMatch(slot -> slot.startTime().equals(startTime) && slot.endTime().equals(endTime));
            if (!slotSupported) {
                throw problem(422, "SLOT_NOT_SUPPORTED", "The requested slot is not operated by the court.", false, null);
            }

            Instant expiresAt = date.atTime(startTime).atZone(COURT_ZONE).toInstant();
            if (!now.isBefore(expiresAt)) {
                throw problem(422, "ALERT_WINDOW_CLOSED", "The alert window has already closed.", false, null);
            }

            Optional<Alert> duplicate = alerts.values().stream()
                    .filter(alert -> alert.hasCondition(ownerId, courtId, date, startTime, endTime))
                    .filter(alert -> alert.status() == AlertStatus.WATCHING)
                    .findFirst();
            if (duplicate.isPresent()) {
                CreateResult result = new CreateResult(duplicate.get().copy(), 200, false);
                idempotency.put(idempotencyKey, new IdempotencyRecord(fingerprint, now, result, false));
                return result;
            }

            Alert alert = Alert.watching(UUID.randomUUID(), ownerId, court.courtId(), court.courtName(),
                    court.reservationUrl(), date, startTime, endTime, now, expiresAt);
            alerts.put(alert.alertId(), alert);
            CreateResult result = new CreateResult(alert.copy(), 201, false);
            idempotency.put(idempotencyKey, new IdempotencyRecord(fingerprint, now, result, false));
            return result;
        } catch (RuntimeException exception) {
            idempotency.remove(idempotencyKey, claim);
            throw exception;
        }
    }

    public List<Alert> listAlerts(String ownerId, AlertStatus status) {
        Instant now = clock.instant();
        List<Alert> selected = alerts.values().stream()
                .filter(alert -> alert.ownerId().equals(ownerId))
                .filter(alert -> status == null || alert.status() == status)
                .map(Alert::copy)
                .sorted(Comparator.comparing(Alert::createdAt).reversed())
                .toList();
        expireDueAlerts(now);
        selected.forEach(alert -> alert.refreshDelayed(now, CHECK_DELAY_SECONDS));
        return selected;
    }

    public Alert getAlert(String ownerId, UUID alertId) {
        Alert alert = ownedAlert(ownerId, alertId);
        Instant now = clock.instant();
        if (alert.isDue(now)) {
            alert.expire();
        }
        alert.refreshDelayed(now, CHECK_DELAY_SECONDS);
        return alert.copy();
    }

    public Alert cancelAlert(String ownerId, UUID alertId) {
        Alert alert = ownedAlert(ownerId, alertId);
        Instant now = clock.instant();
        if (alert.isDue(now)) {
            alert.expire();
        }
        alert.cancel();
        return alert.copy();
    }

    private AvailabilitySnapshot refreshAvailability(String courtId,
                                                      LocalDate date,
                                                      AvailabilitySnapshot previous) {
        try {
            CourtAvailabilityPort.CourtAvailabilityCheck result = availabilityPort.checkAvailability(courtId, date);
            List<AvailabilitySnapshot.Slot> slots = result.slots().stream()
                    .map(slot -> new AvailabilitySnapshot.Slot(slot.startTime(), slot.endTime(), slot.available()))
                    .toList();
            AvailabilitySnapshot snapshot = new AvailabilitySnapshot(result.court().courtId(), result.court().courtName(),
                    result.court().reservationUrl(), result.date(), result.confirmedAt(), false, null, slots);
            String key = availabilityKey(courtId, date);
            availabilitySnapshots.put(key, snapshot);
            availabilityFailures.remove(key);
            recordSuccessfulCheck(snapshot);
            return snapshot;
        } catch (CourtAvailabilityPort.CheckFailed exception) {
            String key = availabilityKey(courtId, date);
            FailureMarker marker = new FailureMarker(exception.reason().name(), clock.instant());
            availabilityFailures.put(key, marker);
            if (previous != null) {
                return previous.stale(exception.reason().name());
            }
            throw problem(statusFor(exception.reason().name()), exception.reason().name(),
                    "The upstream reservation service could not be read.", true, AVAILABILITY_REFRESH_SECONDS);
        } catch (CourtAvailabilityPort.CourtNotSupportedException exception) {
            throw problem(404, "COURT_NOT_SUPPORTED", "The requested court is not supported.", false, null);
        }
    }

    private void recordSuccessfulCheck(AvailabilitySnapshot snapshot) {
        for (Alert alert : alerts.values()) {
            if (alert.status() != AlertStatus.WATCHING
                    || !alert.courtId().equals(snapshot.courtId())
                    || !alert.date().equals(snapshot.date())) {
                continue;
            }
            boolean available = snapshot.slots().stream()
                    .filter(slot -> slot.startTime().equals(alert.startTime()) && slot.endTime().equals(alert.endTime()))
                    .anyMatch(AvailabilitySnapshot.Slot::available);
            alert.recordSuccessfulCheck(snapshot.confirmedAt(), available);
        }
    }

    private AvailabilitySnapshot awaitRefresh(CompletableFuture<AvailabilitySnapshot> future) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw problem(500, "INTERNAL_ERROR", "The availability refresh failed.", true, 5);
        }
    }

    private Alert ownedAlert(String ownerId, UUID alertId) {
        Alert alert = alerts.get(alertId);
        if (alert == null || !alert.ownerId().equals(ownerId)) {
            throw problem(404, "ALERT_NOT_FOUND", "The alert could not be found.", false, null);
        }
        return alert;
    }

    private void expireDueAlerts(Instant now) {
        alerts.values().stream().filter(alert -> alert.isDue(now)).forEach(Alert::expire);
    }

    private void purgeExpiredIdempotency(Instant now) {
        idempotency.entrySet().removeIf(entry -> ageSeconds(entry.getValue().createdAt(), now) >= IDEMPOTENCY_RETENTION_SECONDS);
    }

    private static String availabilityKey(String courtId, LocalDate date) {
        return courtId + "|" + date;
    }

    private static long ageSeconds(Instant then, Instant now) {
        return Math.max(0, Duration.between(then, now).getSeconds());
    }

    private static int statusFor(String reason) {
        return switch (reason) {
            case "UPSTREAM_TIMEOUT" -> 504;
            case "UPSTREAM_RESPONSE_UNREADABLE" -> 502;
            default -> 503;
        };
    }

    private static UseCaseException problem(int status,
                                            String code,
                                            String detail,
                                            boolean retryable,
                                            Integer retryAfterSeconds) {
        return new UseCaseException(status, code, detail, retryable, retryAfterSeconds);
    }

    public record CreateResult(Alert alert, int statusCode, boolean replayed) {
    }

    private record FailureMarker(String reason, Instant failedAt) {
        private UseCaseException asException() {
            return problem(statusFor(reason), reason, "The upstream reservation service is temporarily unavailable.",
                    true, (int) AVAILABILITY_REFRESH_SECONDS);
        }
    }

    private record IdempotencyRecord(String fingerprint, Instant createdAt, CreateResult result, boolean inFlight) {
    }
}
