package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertDelivery;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.DeliveryStatus;
import com.thinking.tennis.port.CourtAvailabilityPort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AlertService {
    private static final ZoneId COURT_ZONE = ZoneId.of("Asia/Seoul");
    private static final long IDEMPOTENCY_RETENTION_HOURS = 24L;
    private static final long CHECK_DELAY_SECONDS = AvailabilityService.FRESHNESS_SECONDS * 3L;

    private final AvailabilityService availabilityService;
    private final ConcurrentMap<String, Alert> alerts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, StoredIdempotentResponse> idempotency = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> idempotencyLocks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> conditionLocks = new ConcurrentHashMap<>();

    public AlertService(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    public CreateAlertResult create(String ownerId, String idempotencyKey, AlertCommand command) {
        Instant now = Instant.now();
        purgeExpiredIdempotency(now);
        String idempotencyId = ownerId + "|" + idempotencyKey;
        String fingerprint = command.fingerprint();
        StoredIdempotentResponse stored = idempotency.get(idempotencyId);
        if (stored != null) {
            if (!stored.requestFingerprint().equals(fingerprint)) {
                throw failure(FailureCode.IDEMPOTENCY_KEY_REUSED, 409, false, null,
                        "이미 다른 요청에 쓰인 멱등 키입니다");
            }
            return new CreateAlertResult(stored.alert(), stored.status(), true);
        }

        if (idempotencyLocks.putIfAbsent(idempotencyId, new Object()) != null) {
            throw failure(FailureCode.CONCURRENT_UPDATE_CONFLICT, 503, true, 1,
                    "같은 멱등 키의 요청이 처리 중입니다");
        }

        try {
            stored = idempotency.get(idempotencyId);
            if (stored != null) {
                if (!stored.requestFingerprint().equals(fingerprint)) {
                    throw failure(FailureCode.IDEMPOTENCY_KEY_REUSED, 409, false, null,
                            "이미 다른 요청에 쓰인 멱등 키입니다");
                }
                return new CreateAlertResult(stored.alert(), stored.status(), true);
            }

            CourtAvailabilityPort.SupportedCourt court = availabilityService.supportedCourt(command.courtId())
                    .orElseThrow(() -> failure(FailureCode.COURT_NOT_SUPPORTED, 422, false, null,
                            "지원하지 않는 코트입니다"));
            availabilityService.cachedSlotSupport(
                    command.courtId(),
                    command.date(),
                    command.startTime(),
                    command.endTime()
            ).ifPresent(supported -> {
                if (!supported) {
                    throw failure(FailureCode.SLOT_NOT_SUPPORTED, 422, false, null,
                            "지원하지 않는 시간대입니다");
                }
            });

            String conditionId = ownerId + "|" + command.fingerprint();
            Object conditionLock = conditionLocks.computeIfAbsent(conditionId, ignored -> new Object());
            synchronized (conditionLock) {
                Alert existing = findWatching(ownerId, command, now);
                if (existing != null) {
                    AlertView view = view(existing, now);
                    StoredIdempotentResponse response = new StoredIdempotentResponse(fingerprint, 200, view, now);
                    idempotency.put(idempotencyId, response);
                    return new CreateAlertResult(view, 200, false);
                }

                Instant createdAt = now;
                Instant expiresAt = ZonedDateTime.of(
                        command.date(),
                        command.startTime(),
                        COURT_ZONE
                ).toInstant();
                if (!expiresAt.isAfter(createdAt)) {
                    throw failure(FailureCode.ALERT_WINDOW_CLOSED, 422, false, null,
                            "이미 지난 시간대입니다");
                }

                Alert alert = new Alert(
                        UUID.randomUUID().toString(),
                        ownerId,
                        court.courtId(),
                        court.courtName(),
                        court.reservationUrl(),
                        command.date(),
                        command.startTime(),
                        command.endTime(),
                        createdAt,
                        expiresAt
                );
                alerts.put(alert.alertId(), alert);
                AlertView view = view(alert, now);
                idempotency.put(idempotencyId,
                        new StoredIdempotentResponse(fingerprint, 201, view, now));
                return new CreateAlertResult(view, 201, false);
            }
        } finally {
            idempotencyLocks.remove(idempotencyId);
        }
    }

    public List<AlertView> list(String ownerId, AlertStatus status) {
        Instant now = Instant.now();
        List<AlertView> result = new ArrayList<>();
        for (Alert alert : alerts.values()) {
            if (!alert.ownerId().equals(ownerId)) {
                continue;
            }
            alert.expireIfDue(now);
            AlertView view = view(alert, now);
            if (status == null || view.status() == status) {
                result.add(view);
            }
        }
        result.sort(Comparator.comparing(AlertView::createdAt).reversed());
        return result;
    }

    public AlertView get(String ownerId, String alertId) {
        Alert alert = ownedAlert(ownerId, alertId);
        Instant now = Instant.now();
        alert.expireIfDue(now);
        return view(alert, now);
    }

    public AlertView cancel(String ownerId, String alertId) {
        Alert alert = ownedAlert(ownerId, alertId);
        synchronized (alert) {
            alert.expireIfDue(Instant.now());
            alert.cancel();
            return view(alert, Instant.now());
        }
    }

    public void recordAvailabilityCheck(String courtId, java.time.LocalDate date, Instant checkedAt) {
        Instant now = Instant.now();
        for (Alert alert : alerts.values()) {
            if (alert.courtId().equals(courtId) && alert.date().equals(date)) {
                alert.recordAvailabilityCheck(checkedAt, now, CHECK_DELAY_SECONDS);
            }
        }
    }

    public void setDelivery(String alertId, AlertDelivery delivery) {
        Alert alert = alerts.get(alertId);
        if (alert != null) {
            alert.setDelivery(delivery);
        }
    }

    private Alert ownedAlert(String ownerId, String alertId) {
        Alert alert = alerts.get(alertId);
        if (alert == null || !alert.ownerId().equals(ownerId)) {
            throw failure(FailureCode.ALERT_NOT_FOUND, 404, false, null,
                    "신청을 찾을 수 없습니다");
        }
        return alert;
    }

    private Alert findWatching(String ownerId, AlertCommand command, Instant now) {
        for (Alert alert : alerts.values()) {
            if (!alert.ownerId().equals(ownerId)) {
                continue;
            }
            alert.expireIfDue(now);
            if (alert.isWatching()
                    && alert.courtId().equals(command.courtId())
                    && alert.date().equals(command.date())
                    && alert.startTime().equals(command.startTime())
                    && alert.endTime().equals(command.endTime())) {
                return alert;
            }
        }
        return null;
    }

    private AlertView view(Alert alert, Instant now) {
        alert.expireIfDue(now);
        Instant lastCheckedAt = alert.lastCheckedAt();
        boolean delayed = lastCheckedAt == null
                ? alert.createdAt().plusSeconds(CHECK_DELAY_SECONDS).isBefore(now)
                : lastCheckedAt.plusSeconds(CHECK_DELAY_SECONDS).isBefore(now);
        return new AlertView(
                alert.alertId(),
                alert.courtId(),
                alert.courtName(),
                alert.reservationUrl(),
                alert.date(),
                alert.startTime(),
                alert.endTime(),
                alert.status(),
                alert.createdAt(),
                alert.expiresAt(),
                lastCheckedAt,
                delayed,
                copyDelivery(alert.delivery())
        );
    }

    private AlertDelivery copyDelivery(AlertDelivery delivery) {
        if (delivery == null) {
            return null;
        }
        return new AlertDelivery(
                delivery.status(),
                delivery.attemptCount(),
                delivery.lastAttemptAt(),
                delivery.failureReason()
        );
    }

    private void purgeExpiredIdempotency(Instant now) {
        Instant cutoff = now.minus(Duration.ofHours(IDEMPOTENCY_RETENTION_HOURS));
        idempotency.entrySet().removeIf(entry -> entry.getValue().createdAt().isBefore(cutoff));
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

    public record AlertView(
            String alertId,
            String courtId,
            String courtName,
            String reservationUrl,
            java.time.LocalDate date,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime,
            AlertStatus status,
            Instant createdAt,
            Instant expiresAt,
            Instant lastCheckedAt,
            boolean checkDelayed,
            AlertDelivery delivery
    ) {
    }

    private record StoredIdempotentResponse(
            String requestFingerprint,
            int status,
            AlertView alert,
            Instant createdAt
    ) {
    }
}
