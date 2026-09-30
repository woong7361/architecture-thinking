package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.AvailabilitySnapshot;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AlertApplicationService {

    private static final ZoneId COURT_ZONE = ZoneId.of("Asia/Seoul");
    private static final String CREATE_OPERATION = "createAlert";
    private static final Duration CHECK_DELAY_THRESHOLD = Duration.ofSeconds(60);

    private final InMemoryAlertStore alertStore;
    private final AvailabilityService availabilityService;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AlertApplicationService(
            InMemoryAlertStore alertStore,
            AvailabilityService availabilityService) {
        this(alertStore, availabilityService, Clock.systemUTC());
    }

    AlertApplicationService(
            InMemoryAlertStore alertStore,
            AvailabilityService availabilityService,
            Clock clock) {
        this.alertStore = alertStore;
        this.availabilityService = availabilityService;
        this.clock = clock;
    }

    public CreateResult create(
            String ownerId,
            String idempotencyKey,
            String courtId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime) {
        Instant now = clock.instant();
        String fingerprint = fingerprint(courtId, date, startTime, endTime);
        InMemoryAlertStore.IdempotencyLookup lookup =
                alertStore.beginIdempotentRequest(ownerId, CREATE_OPERATION, idempotencyKey, fingerprint, now);
        if (lookup.kind() == InMemoryAlertStore.IdempotencyLookup.Kind.REUSED) {
            throw new UseCaseException(
                    UseCaseException.Code.IDEMPOTENCY_KEY_REUSED,
                    "이미 다른 요청에 쓰인 멱등 키입니다");
        }
        if (lookup.kind() == InMemoryAlertStore.IdempotencyLookup.Kind.IN_FLIGHT) {
            throw new UseCaseException(
                    UseCaseException.Code.CONCURRENT_UPDATE_CONFLICT,
                    "같은 요청이 아직 처리 중입니다",
                    1);
        }
        if (lookup.kind() == InMemoryAlertStore.IdempotencyLookup.Kind.REPLAYED) {
            return new CreateResult(lookup.alert(), false, true);
        }

        try {
            Alert existing = alertStore.findActiveByCondition(ownerId, courtId, date, startTime, endTime);
            if (existing != null) {
                alertStore.completeIdempotentRequest(ownerId, CREATE_OPERATION, idempotencyKey, existing, now);
                return new CreateResult(existing, false, false);
            }

            AvailabilitySnapshot snapshot = availabilityService.get(date, courtId, now);
            availabilityService.requireSlot(snapshot, startTime, endTime);
            Instant expiresAt = date.atTime(startTime).atZone(COURT_ZONE).toInstant();
            if (!expiresAt.isAfter(now)) {
                throw new UseCaseException(
                        UseCaseException.Code.ALERT_WINDOW_CLOSED,
                        "이용 시작 시각이 지나 감시할 수 없습니다");
            }
            Alert alert = new Alert(
                    UUID.randomUUID(),
                    ownerId,
                    snapshot.court().courtId(),
                    snapshot.court().courtName(),
                    snapshot.court().reservationUrl(),
                    date,
                    startTime,
                    endTime,
                    AlertStatus.WATCHING,
                    now,
                    expiresAt,
                    null,
                    false,
                    null);
            alertStore.save(alert);
            alertStore.completeIdempotentRequest(ownerId, CREATE_OPERATION, idempotencyKey, alert, now);
            return new CreateResult(alert, true, false);
        } catch (RuntimeException exception) {
            alertStore.abandonIdempotentRequest(ownerId, CREATE_OPERATION, idempotencyKey);
            throw exception;
        }
    }

    public List<Alert> list(String ownerId, AlertStatus status) {
        return alertStore.findByOwner(ownerId, status);
    }

    public Alert get(String ownerId, UUID alertId) {
        Alert alert = ownedAlert(ownerId, alertId);
        return projectCheckDelay(alert, clock.instant());
    }

    public Alert cancel(String ownerId, UUID alertId) {
        Instant now = clock.instant();
        Alert alert = ownedAlert(ownerId, alertId);
        if (alert.status() == AlertStatus.WATCHING) {
            alert = alert.withStatus(AlertStatus.CANCELED);
            alertStore.save(alert);
        }
        return projectCheckDelay(alert, now);
    }

    public void expireDueAlerts() {
        expireDueAlerts(clock.instant());
    }

    private void expireDueAlerts(Instant now) {
        alertStore.expireBefore(now);
    }

    private Alert ownedAlert(String ownerId, UUID alertId) {
        Alert alert = alertStore.find(alertId);
        if (alert == null || !alert.ownerId().equals(ownerId)) {
            throw new UseCaseException(
                    UseCaseException.Code.ALERT_NOT_FOUND,
                    "신청을 찾을 수 없습니다");
        }
        return alert;
    }

    private Alert projectCheckDelay(Alert alert, Instant now) {
        if (alert.status() != AlertStatus.WATCHING) {
            return alert;
        }
        boolean delayed = alert.lastCheckedAt() == null
                ? Duration.between(alert.createdAt(), now).compareTo(CHECK_DELAY_THRESHOLD) > 0
                : Duration.between(alert.lastCheckedAt(), now)
                .compareTo(CHECK_DELAY_THRESHOLD) > 0;
        return alert.withCheckState(alert.lastCheckedAt(), delayed);
    }

    private String fingerprint(
            String courtId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime) {
        String input = courtId + "|" + date + "|" + startTime + "|" + endTime;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }

    public record CreateResult(Alert alert, boolean created, boolean replayed) {
    }
}
