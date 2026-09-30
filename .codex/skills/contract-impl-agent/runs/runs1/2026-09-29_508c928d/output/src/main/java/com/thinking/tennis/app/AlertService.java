package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertSnapshot;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.Availability;
import com.thinking.tennis.domain.DeliveryStatus;
import com.thinking.tennis.domain.TimeSlot;
import com.thinking.tennis.port.CourtAvailabilityPort;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class AlertService {
    private static final ZoneId COURT_ZONE = ZoneId.of("Asia/Seoul");
    private static final int IDEMPOTENCY_RETENTION_HOURS = 24;
    private static final int RETRY_AFTER_SECONDS = 1;

    private final AvailabilityService availabilityService;
    private final Map<UUID, Alert> alerts = new ConcurrentHashMap<>();
    private final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();
    private final Map<UUID, ReentrantLock> alertLocks = new ConcurrentHashMap<>();
    private final Map<String, Object> conditionLocks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService expirationExecutor;

    public AlertService(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
        this.expirationExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "tennis-alert-expiration");
            thread.setDaemon(true);
            return thread;
        });
        this.expirationExecutor.scheduleWithFixedDelay(
                this::expireAlerts,
                1,
                1,
                TimeUnit.SECONDS
        );
    }

    public CreateAlertResult create(String userId, String idempotencyKey, CreateAlertCommand command) {
        String fingerprint = fingerprint(command);
        String recordKey = userId + "|" + idempotencyKey;
        Instant now = Instant.now();
        IdempotencyRecord existing = idempotency.get(recordKey);
        if (existing != null && !existing.expiresAt().isAfter(now)) {
            idempotency.remove(recordKey, existing);
            existing = null;
        }
        if (existing != null && existing.expiresAt().isAfter(now)) {
            if (!existing.fingerprint().equals(fingerprint)) {
                throw failure(FailureCode.IDEMPOTENCY_KEY_REUSED, FailureContext.ALERT_REQUEST,
                        "같은 Idempotency-Key로 내용이 다른 요청이 앞서 처리됐습니다.", false, null);
            }
            if (existing.inProgress()) {
                throw failure(FailureCode.CONCURRENT_UPDATE_CONFLICT, FailureContext.ALERT_REQUEST,
                        "같은 요청이 아직 처리 중입니다.", true, RETRY_AFTER_SECONDS);
            }
            return new CreateAlertResult(existing.alert(), existing.status(), true);
        }

        IdempotencyRecord reservation = IdempotencyRecord.inProgress(
                fingerprint,
                now.plusSeconds(IDEMPOTENCY_RETENTION_HOURS * 60L * 60L)
        );
        IdempotencyRecord raced = idempotency.putIfAbsent(recordKey, reservation);
        if (raced != null) {
            if (!raced.fingerprint().equals(fingerprint)) {
                throw failure(FailureCode.IDEMPOTENCY_KEY_REUSED, FailureContext.ALERT_REQUEST,
                        "같은 Idempotency-Key로 내용이 다른 요청이 앞서 처리됐습니다.", false, null);
            }
            throw failure(FailureCode.CONCURRENT_UPDATE_CONFLICT, FailureContext.ALERT_REQUEST,
                    "같은 요청이 아직 처리 중입니다.", true, RETRY_AFTER_SECONDS);
        }

        try {
            CreateAlertResult result = createNewOrReuse(userId, command, now);
            idempotency.put(recordKey, reservation.completed(result.alert(), result.status()));
            return result;
        } catch (RuntimeException exception) {
            idempotency.remove(recordKey, reservation);
            throw exception;
        }
    }

    public List<AlertSnapshot> list(String userId, Optional<AlertStatus> status) {
        Instant now = Instant.now();
        return alerts.values().stream()
                .filter(alert -> alert.userId().equals(userId))
                .filter(alert -> status.isEmpty() || alert.snapshot(now).status() == status.get())
                .map(alert -> alert.snapshot(now))
                .sorted(Comparator.comparing(AlertSnapshot::createdAt).reversed())
                .toList();
    }

    public AlertSnapshot get(String userId, UUID alertId) {
        Alert alert = ownedAlert(userId, alertId);
        return alert.snapshot(Instant.now());
    }

    public AlertSnapshot cancel(String userId, UUID alertId) {
        Alert alert = ownedAlert(userId, alertId);
        ReentrantLock lock = alertLocks.computeIfAbsent(alertId, ignored -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw failure(FailureCode.CONCURRENT_UPDATE_CONFLICT, FailureContext.ALERT_RESOURCE,
                    "같은 신청을 동시에 고치려는 요청이 있었습니다.", true, RETRY_AFTER_SECONDS);
        }
        try {
            alert.cancel();
            return alert.snapshot(Instant.now());
        } finally {
            lock.unlock();
        }
    }

    private CreateAlertResult createNewOrReuse(
            String userId,
            CreateAlertCommand command,
            Instant now
    ) {
        Optional<Alert> alreadyWatching = findWatching(userId, command, now);
        if (alreadyWatching.isPresent()) {
            return new CreateAlertResult(alreadyWatching.get().snapshot(now), 200, false);
        }

        AvailabilityResult availabilityResult;
        try {
            availabilityResult = availabilityService.get(command.courtId(), command.date());
        } catch (UseCaseException exception) {
            if (exception.code() == FailureCode.COURT_NOT_SUPPORTED) {
                throw failure(FailureCode.COURT_NOT_SUPPORTED, FailureContext.ALERT_REQUEST,
                        exception.getMessage(), false, null);
            }
            if (exception.code() == FailureCode.UPSTREAM_TIMEOUT
                    || exception.code() == FailureCode.UPSTREAM_UNAVAILABLE
                    || exception.code() == FailureCode.UPSTREAM_RESPONSE_UNREADABLE) {
                throw failure(FailureCode.INTERNAL_ERROR, FailureContext.ALERT_REQUEST,
                        "요청을 처리하지 못했습니다.", true, 5);
            }
            throw exception;
        }
        Availability availability = availabilityResult.availability();
        Availability.Slot requestedSlot = availability.slots().stream()
                .filter(slot -> slot.startTime().equals(command.slot().startTime())
                        && slot.endTime().equals(command.slot().endTime()))
                .findFirst()
                .orElseThrow(() -> failure(FailureCode.SLOT_NOT_SUPPORTED, FailureContext.ALERT_REQUEST,
                        "요청한 시간대는 그 날짜에 운영하지 않습니다.", false, null));

        Instant expiresAt = command.date()
                .atTime(command.slot().startTime())
                .atZone(COURT_ZONE)
                .toInstant();
        if (!expiresAt.isAfter(now)) {
            throw failure(FailureCode.ALERT_WINDOW_CLOSED, FailureContext.ALERT_REQUEST,
                    "이용 시작 시각이 지나 감시할 수 없습니다.", false, null);
        }

        String conditionKey = userId + "|" + fingerprint(command);
        synchronized (conditionLocks.computeIfAbsent(conditionKey, ignored -> new Object())) {
            Optional<Alert> watching = findWatching(userId, command, now);
            if (watching.isPresent()) {
                return new CreateAlertResult(watching.get().snapshot(now), 200, false);
            }

            UUID alertId = UUID.randomUUID();
            Alert alert = new Alert(
                    alertId,
                    userId,
                    availability.courtId(),
                    availability.courtName(),
                    availability.reservationUrl(),
                    command.date(),
                    command.slot(),
                    now,
                    expiresAt
            );
            alerts.put(alertId, alert);
            return new CreateAlertResult(alert.snapshot(now), 201, false);
        }
    }

    private Optional<Alert> findWatching(String userId, CreateAlertCommand command, Instant now) {
        return alerts.values().stream()
                .filter(alert -> alert.userId().equals(userId))
                .filter(alert -> alert.snapshot(now).status() == AlertStatus.WATCHING)
                .filter(alert -> alert.courtId().equals(command.courtId()))
                .filter(alert -> alert.date().equals(command.date()))
                .filter(alert -> alert.slot().equals(command.slot()))
                .findFirst();
    }

    private void expireAlerts() {
        Instant now = Instant.now();
        alerts.values().forEach(alert -> alert.expire(now));
    }

    @PreDestroy
    void stopExpirationExecutor() {
        expirationExecutor.shutdownNow();
    }

    private Alert ownedAlert(String userId, UUID alertId) {
        Alert alert = alerts.get(alertId);
        if (alert == null || !alert.userId().equals(userId)) {
            throw failure(FailureCode.ALERT_NOT_FOUND, FailureContext.ALERT_RESOURCE,
                    "신청을 찾을 수 없습니다.", false, null);
        }
        return alert;
    }

    private String fingerprint(CreateAlertCommand command) {
        return command.courtId() + "|" + command.date() + "|"
                + command.slot().startTime() + "|" + command.slot().endTime();
    }

    private UseCaseException failure(
            FailureCode code,
            FailureContext context,
            String detail,
            boolean retryable,
            Integer retryAfterSeconds
    ) {
        return new UseCaseException(code, context, detail, retryable, retryAfterSeconds);
    }

    private record IdempotencyRecord(
            String fingerprint,
            Instant expiresAt,
            boolean inProgress,
            AlertSnapshot alert,
            int status
    ) {
        static IdempotencyRecord inProgress(String fingerprint, Instant expiresAt) {
            return new IdempotencyRecord(fingerprint, expiresAt, true, null, 0);
        }

        IdempotencyRecord completed(AlertSnapshot alert, int status) {
            return new IdempotencyRecord(fingerprint, expiresAt, false, alert, status);
        }
    }
}
