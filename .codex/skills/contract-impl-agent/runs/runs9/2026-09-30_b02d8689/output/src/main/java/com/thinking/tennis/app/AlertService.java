package com.thinking.tennis.app;

import com.thinking.tennis.app.AppExceptions.AlertNotFound;
import com.thinking.tennis.app.AppExceptions.AlertWindowClosed;
import com.thinking.tennis.app.AppExceptions.ConcurrentUpdateConflict;
import com.thinking.tennis.app.AppExceptions.IdempotencyKeyReused;
import com.thinking.tennis.app.AppExceptions.SlotNotSupported;
import com.thinking.tennis.app.AppExceptions.UpstreamUnavailable;
import com.thinking.tennis.app.AppExceptions.ErrorCode;
import com.thinking.tennis.app.AppExceptions.AppException;
import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.TimeSlot;
import com.thinking.tennis.port.CourtAvailabilityPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    private final AvailabilityService availabilityService;
    private final InMemoryAlertStore alertStore;
    private final Clock clock;
    private final ConcurrentHashMap<IdempotencyKey, IdempotencyRecord> idempotencyRecords = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<IdempotencyKey, Boolean> inProgress = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<AlertCondition, Object> conditionLocks = new ConcurrentHashMap<>();

    public AlertService(AvailabilityService availabilityService, InMemoryAlertStore alertStore) {
        this.availabilityService = availabilityService;
        this.alertStore = alertStore;
        this.clock = Clock.systemUTC();
    }

    public CreateAlertResult createAlert(String userId, UUID idempotencyKey, CreateAlertCommand command) {
        alertStore.expireDue(clock.instant());
        IdempotencyKey key = new IdempotencyKey(userId, "createAlert", idempotencyKey);
        IdempotencyRecord stored = idempotencyRecords.get(key);
        if (stored != null && stored.isFresh(clock.instant())) {
            if (!stored.request().equals(command)) {
                throw new IdempotencyKeyReused();
            }
            return stored.result().asReplayed();
        }

        if (inProgress.putIfAbsent(key, Boolean.TRUE) != null) {
            throw new ConcurrentUpdateConflict();
        }
        try {
            CreateAlertResult result = createAlertOnce(userId, command);
            idempotencyRecords.put(key, new IdempotencyRecord(
                    command,
                    result.statusCode(),
                    result.snapshot(),
                    result.checkDelayed(),
                    clock.instant()));
            return result;
        } finally {
            inProgress.remove(key);
            evictExpiredIdempotencyRecords();
        }
    }

    public List<Alert> listAlerts(String userId, AlertStatus status) {
        alertStore.expireDue(clock.instant());
        return alertStore.listForUser(userId, status);
    }

    public Alert getAlert(String userId, UUID alertId) {
        alertStore.expireDue(clock.instant());
        return alertStore.findForUser(userId, alertId).orElseThrow(AlertNotFound::new);
    }

    public Alert cancelAlert(String userId, UUID alertId) {
        alertStore.expireDue(clock.instant());
        Alert alert = alertStore.findForUser(userId, alertId).orElseThrow(AlertNotFound::new);
        alert.cancel();
        return alert;
    }

    private CreateAlertResult createAlertOnce(String userId, CreateAlertCommand command) {
        AlertCondition condition = new AlertCondition(userId, command.courtId(), command.date(), command.slot());
        Object lock = conditionLocks.computeIfAbsent(condition, ignored -> new Object());
        synchronized (lock) {
            var existing = alertStore.findWatchingByCondition(userId, command.courtId(), command.date(), command.slot());
            if (existing.isPresent()) {
                return new CreateAlertResult(200, existing.get(), false, false);
            }
            Instant expiresAt = command.date().atTime(command.slot().startTime()).atZone(TennisAlertConstants.COURT_ZONE).toInstant();
            if (!expiresAt.isAfter(clock.instant())) {
                throw new AlertWindowClosed();
            }
            CourtAvailabilityPort.SupportedCourt court = availabilityService.findCourt(command.courtId());
            try {
                if (!availabilityService.isSlotSupported(command.courtId(), command.date(), command.slot())) {
                    throw new SlotNotSupported(command.courtId() + "은 그 날짜에 요청한 시간대로 운영하지 않습니다");
                }
            } catch (UpstreamUnavailable ex) {
                throw new AppException(ErrorCode.INTERNAL_ERROR, ex.getMessage());
            }
            return newAlert(userId, command, court, expiresAt).get();
        }
    }

    private Supplier<CreateAlertResult> newAlert(String userId,
                                                 CreateAlertCommand command,
                                                 CourtAvailabilityPort.SupportedCourt court,
                                                 Instant expiresAt) {
        return () -> {
            Alert alert = new Alert(
                    UUID.randomUUID(),
                    userId,
                    court.courtId(),
                    court.courtName(),
                    court.reservationUrl(),
                    command.date(),
                    command.slot(),
                    AlertStatus.WATCHING,
                    clock.instant(),
                    expiresAt,
                    null,
                    null);
            alertStore.save(alert);
            return new CreateAlertResult(201, alert, false, false);
        };
    }

    private void evictExpiredIdempotencyRecords() {
        Instant now = clock.instant();
        idempotencyRecords.entrySet().removeIf(entry -> !entry.getValue().isFresh(now));
    }

    public record CreateAlertCommand(String courtId, LocalDate date, TimeSlot slot) {
    }

    public record CreateAlertResult(int statusCode, Alert alert, boolean replayed, boolean checkDelayed) {
        CreateAlertResult asReplayed() {
            return new CreateAlertResult(statusCode, alert, true, checkDelayed);
        }

        Alert snapshot() {
            var delivery = alert.delivery();
            var deliveryCopy = delivery == null
                    ? null
                    : new com.thinking.tennis.domain.AlertDelivery(
                            delivery.status(),
                            delivery.attemptCount(),
                            delivery.lastAttemptAt(),
                            delivery.failureReason());
            return new Alert(
                    alert.alertId(),
                    alert.userId(),
                    alert.courtId(),
                    alert.courtName(),
                    alert.reservationUrl(),
                    alert.date(),
                    alert.slot(),
                    alert.status(),
                    alert.createdAt(),
                    alert.expiresAt(),
                    alert.lastCheckedAt(),
                    deliveryCopy);
        }
    }

    private record IdempotencyKey(String userId, String operation, UUID value) {
    }

    private record AlertCondition(String userId, String courtId, LocalDate date, TimeSlot slot) {
    }

    private record IdempotencyRecord(CreateAlertCommand request,
                                     int statusCode,
                                     Alert snapshot,
                                     boolean checkDelayed,
                                     Instant storedAt) {
        boolean isFresh(Instant now) {
            return storedAt.plus(TennisAlertConstants.IDEMPOTENCY_RETENTION).isAfter(now);
        }

        CreateAlertResult result() {
            return new CreateAlertResult(statusCode, snapshot, false, checkDelayed);
        }
    }
}
