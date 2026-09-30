package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertSnapshot;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.AvailabilitySnapshot;
import com.thinking.tennis.port.CourtAvailabilityPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertApplicationService {
    private static final ZoneId COURT_ZONE = ZoneId.of("Asia/Seoul");
    private static final Duration IDEMPOTENCY_RETENTION = Duration.ofHours(24);

    private final AvailabilityApplicationService availabilityService;
    private final ConcurrentHashMap<UUID, Alert> alerts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ConditionKey, Object> conditionLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletedRequest> completedRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<CreateResult>> inFlightRequests = new ConcurrentHashMap<>();

    public AlertApplicationService(AvailabilityApplicationService availabilityService) {
        this.availabilityService = availabilityService;
    }

    public CreateResult create(String userId, String idempotencyKey, CreateCommand command) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ValidationFailure("Idempotency-Key is required.");
        }
        if (!isUuid(idempotencyKey)) {
            throw new ValidationFailure("Idempotency-Key must be a UUID.");
        }

        CompletedRequest completed = completedRequest(idempotencyKey);
        if (completed != null) {
            if (!completed.command().equals(command) || !completed.userId().equals(userId)) {
                throw ApiFailure.idempotencyReused();
            }
            return new CreateResult(completed.alert(), completed.created(), true);
        }

        CompletableFuture<CreateResult> marker = new CompletableFuture<>();
        if (inFlightRequests.putIfAbsent(idempotencyKey, marker) != null) {
            throw ApiFailure.contention();
        }
        try {
            completed = completedRequest(idempotencyKey);
            if (completed != null) {
                if (!completed.command().equals(command) || !completed.userId().equals(userId)) {
                    throw ApiFailure.idempotencyReused();
                }
                return new CreateResult(completed.alert(), completed.created(), true);
            }
            CreateResult result = createWithoutIdempotency(userId, command);
            completedRequests.put(idempotencyKey, new CompletedRequest(
                    userId, command, result.alert(), result.created(),
                    Instant.now().plus(IDEMPOTENCY_RETENTION)));
            marker.complete(result);
            return result;
        } finally {
            inFlightRequests.remove(idempotencyKey, marker);
        }
    }

    private CreateResult createWithoutIdempotency(String userId, CreateCommand command) {
        LocalTime startTime = command.startTime();
        LocalTime endTime = command.endTime();
        ConditionKey key = new ConditionKey(userId, command.courtId(), command.date(), startTime, endTime);
        Object conditionLock = conditionLocks.computeIfAbsent(key, ignored -> new Object());

        synchronized (conditionLock) {
            Instant now = Instant.now();
            for (Alert existing : alerts.values()) {
                if (existing.matches(userId, command.courtId(), command.date(), startTime, endTime, now)) {
                    return new CreateResult(existing.snapshot(now), false, false);
                }
            }

            AvailabilitySnapshot availability = availabilityService.getForAlert(command.courtId(), command.date());
            boolean supported = availability.slots().stream()
                    .anyMatch(slot -> slot.startTime().equals(startTime) && slot.endTime().equals(endTime));
            if (!supported) {
                throw ApiFailure.slotNotSupported();
            }

            Instant expiresAt = command.date().atTime(startTime).atZone(COURT_ZONE).toInstant();
            if (!now.isBefore(expiresAt)) {
                throw ApiFailure.windowClosed();
            }

            UUID alertId = UUID.randomUUID();
            Alert alert = new Alert(
                    alertId,
                    userId,
                    command.courtId(),
                    availability.court().courtName(),
                    availability.court().reservationUrl(),
                    command.date(),
                    startTime,
                    endTime,
                    now,
                    expiresAt);
            alerts.put(alertId, alert);
            alert.applyAvailability(availability, now);
            return new CreateResult(alert.snapshot(now), true, false);
        }
    }

    public List<AlertSnapshot> list(String userId, AlertStatus requestedStatus) {
        Instant now = Instant.now();
        List<AlertSnapshot> result = new ArrayList<>();
        for (Alert alert : alerts.values()) {
            if (!alert.userId().equals(userId)) {
                continue;
            }
            AlertSnapshot snapshot = alert.snapshot(now);
            if (requestedStatus == null || snapshot.status() == requestedStatus) {
                result.add(snapshot);
            }
        }
        result.sort(Comparator.comparing(AlertSnapshot::createdAt).reversed());
        return result;
    }

    public AlertSnapshot get(String userId, UUID alertId) {
        Alert alert = alerts.get(alertId);
        if (alert == null || !alert.userId().equals(userId)) {
            throw ApiFailure.notFound();
        }
        return alert.snapshot(Instant.now());
    }

    public AlertSnapshot cancel(String userId, UUID alertId) {
        Alert alert = alerts.get(alertId);
        if (alert == null || !alert.userId().equals(userId)) {
            throw ApiFailure.notFound();
        }
        return alert.cancel(Instant.now());
    }

    public void applyAvailability(AvailabilitySnapshot snapshot) {
        Instant now = Instant.now();
        for (Alert alert : alerts.values()) {
            if (alert.courtId().equals(snapshot.court().courtId()) && alert.date().equals(snapshot.date())) {
                alert.applyAvailability(snapshot, now);
            }
        }
    }

    @Scheduled(fixedDelay = 1000L)
    public void expireDueAlerts() {
        Instant now = Instant.now();
        for (Alert alert : alerts.values()) {
            alert.expireIfDue(now);
        }
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private CompletedRequest completedRequest(String idempotencyKey) {
        CompletedRequest completed = completedRequests.get(idempotencyKey);
        if (completed == null) {
            return null;
        }
        if (completed.expiresAt().isAfter(Instant.now())) {
            return completed;
        }
        completedRequests.remove(idempotencyKey, completed);
        return null;
    }

    public record CreateCommand(
            String courtId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }

    public record CreateResult(AlertSnapshot alert, boolean created, boolean replayed) {
    }

    private record ConditionKey(
            String userId,
            String courtId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }

    private record CompletedRequest(
            String userId,
            CreateCommand command,
            AlertSnapshot alert,
            boolean created,
            Instant expiresAt
    ) {
    }
}
