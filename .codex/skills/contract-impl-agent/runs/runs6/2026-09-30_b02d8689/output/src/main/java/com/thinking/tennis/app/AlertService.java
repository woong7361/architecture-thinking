package com.thinking.tennis.app;

import com.thinking.tennis.api.ApiModels;
import com.thinking.tennis.port.CourtAvailabilityPort;
import com.thinking.tennis.port.CourtAvailabilityPort.CourtAvailabilityCheck;
import com.thinking.tennis.port.CourtAvailabilityPort.SlotAvailability;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    private static final ZoneId COURT_ZONE = ZoneId.of("Asia/Seoul");
    private static final long IDEMPOTENCY_TTL_SECONDS = 24 * 60 * 60;

    private final AvailabilityService availabilityService;
    private final Clock clock;
    private final Map<UUID, StoredAlert> alerts = new ConcurrentHashMap<>();
    private final Map<IdempotencyKey, StoredResponse> idempotency = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public AlertService(AvailabilityService availabilityService) {
        this(availabilityService, Clock.systemUTC());
    }

    AlertService(AvailabilityService availabilityService, Clock clock) {
        this.availabilityService = availabilityService;
        this.clock = clock;
    }

    public CreateResult createAlert(String subject, String idempotencyKey, ApiModels.AlertRequest request) {
        Instant now = clock.instant();
        String fingerprint = fingerprint(request);
        IdempotencyKey replayKey = new IdempotencyKey(subject, "createAlert", idempotencyKey);
        StoredResponse previous = idempotency.get(replayKey);
        if (previous != null && previous.expiresAt().isAfter(now)) {
            if (!previous.fingerprint().equals(fingerprint)) {
                throw ApplicationException.idempotencyKeyReused();
            }
            return new CreateResult(previous.body(), previous.status(), true);
        }
        if (previous != null) {
            idempotency.remove(replayKey, previous);
        }

        LocalTime requestedStart = LocalTime.parse(request.slot().startTime());
        if (!expiryAt(request.date(), requestedStart).isAfter(now)) {
            throw ApplicationException.windowClosed();
        }

        AvailabilityService.CachedResult availability;
        try {
            availability = availabilityService.getAvailability(request.courtId(), request.date());
        } catch (ApplicationException exception) {
            if (exception.code() == ErrorCode.COURT_NOT_SUPPORTED) {
                throw ApplicationException.courtNotSupported(
                        "courtId: " + request.courtId(),
                        org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY);
            }
            throw exception;
        }
        CourtAvailabilityCheck check = availability.check();
        if (!hasExactSlot(check.slots(), request.slot())) {
            throw ApplicationException.slotNotSupported(
                    check.court().courtId() + "은 그 날짜에 운영하지 않는 시간대입니다");
        }

        expireDueAlerts(now);
        StoredAlert existing = alerts.values().stream()
                .filter(alert -> alert.subject.equals(subject))
                .filter(alert -> alert.status == AlertStatus.WATCHING)
                .filter(alert -> alert.courtId.equals(request.courtId()))
                .filter(alert -> alert.date.equals(request.date()))
                .filter(alert -> alert.startTime.equals(requestedStart))
                .filter(alert -> alert.endTime.equals(LocalTime.parse(request.slot().endTime())))
                .findFirst()
                .orElse(null);

        StoredAlert alert = existing == null ? createStoredAlert(subject, request, check, now) : existing;
        if (existing == null) {
            alerts.put(alert.alertId, alert);
        }
        applyAvailability(alert, availability, now);
        ApiModels.AlertResponse response = toResponse(alert, now);
        int status = existing == null ? 201 : 200;
        idempotency.put(replayKey, new StoredResponse(
                fingerprint, response, status, now.plusSeconds(IDEMPOTENCY_TTL_SECONDS)));
        return new CreateResult(response, status, false);
    }

    public List<ApiModels.AlertResponse> listAlerts(String subject, String status) {
        Instant now = clock.instant();
        expireDueAlerts(now);
        return alerts.values().stream()
                .filter(alert -> alert.subject.equals(subject))
                .filter(alert -> status == null || alert.status.name().equals(status))
                .sorted(Comparator.comparing((StoredAlert alert) -> alert.createdAt).reversed())
                .map(alert -> toResponse(alert, now))
                .toList();
    }

    public ApiModels.AlertResponse getAlert(String subject, UUID alertId) {
        Instant now = clock.instant();
        expireDueAlerts(now);
        StoredAlert alert = alerts.get(alertId);
        if (alert == null || !alert.subject.equals(subject)) {
            throw ApplicationException.alertNotFound();
        }
        return toResponse(alert, now);
    }

    public ApiModels.AlertResponse cancelAlert(String subject, UUID alertId) {
        Instant now = clock.instant();
        expireDueAlerts(now);
        StoredAlert alert = alerts.get(alertId);
        if (alert == null || !alert.subject.equals(subject)) {
            throw ApplicationException.alertNotFound();
        }
        if (alert.status == AlertStatus.WATCHING) {
            alert.status = AlertStatus.CANCELED;
            alert.delivery = null;
        }
        return toResponse(alert, now);
    }

    private StoredAlert createStoredAlert(String subject, ApiModels.AlertRequest request,
                                          CourtAvailabilityCheck check, Instant now) {
        StoredAlert alert = new StoredAlert();
        alert.alertId = UUID.randomUUID();
        alert.subject = subject;
        alert.courtId = request.courtId();
        alert.courtName = check.court().courtName();
        alert.reservationUrl = check.court().reservationUrl();
        alert.date = request.date();
        alert.startTime = LocalTime.parse(request.slot().startTime());
        alert.endTime = LocalTime.parse(request.slot().endTime());
        alert.status = AlertStatus.WATCHING;
        alert.createdAt = now;
        alert.expiresAt = expiryAt(request.date(), alert.startTime);
        alert.lastCheckedAt = check.confirmedAt();
        alert.delivery = null;
        return alert;
    }

    private void applyAvailability(StoredAlert alert, AvailabilityService.CachedResult availability, Instant now) {
        alert.lastCheckedAt = availability.check().confirmedAt();
        if (alert.status != AlertStatus.WATCHING) {
            return;
        }
        boolean available = hasAvailableSlot(availability.check().slots(), alert.startTime, alert.endTime);
        if (available && alert.delivery == null) {
            alert.delivery = new DeliveryState(DeliveryStatus.PENDING, 0, null, null);
        } else if (!available && alert.delivery != null
                && alert.delivery.status == DeliveryStatus.PENDING) {
            alert.delivery = null;
        }
    }

    private void expireDueAlerts(Instant now) {
        alerts.values().stream()
                .filter(alert -> alert.status == AlertStatus.WATCHING)
                .filter(alert -> !alert.expiresAt.isAfter(now))
                .forEach(alert -> {
                    alert.status = AlertStatus.EXPIRED;
                    alert.delivery = null;
                });
    }

    private ApiModels.AlertResponse toResponse(StoredAlert alert, Instant now) {
        ApiModels.DeliveryResponse delivery = alert.delivery == null ? null :
                new ApiModels.DeliveryResponse(
                        alert.delivery.status.name(),
                        alert.delivery.attemptCount,
                        alert.delivery.lastAttemptAt,
                        alert.delivery.failureReason);
        return new ApiModels.AlertResponse(
                alert.alertId.toString(),
                alert.courtId,
                alert.courtName,
                alert.reservationUrl,
                alert.date,
                new ApiModels.TimeSlotRequest(alert.startTime.toString(), alert.endTime.toString()),
                alert.status.name(),
                alert.createdAt,
                alert.expiresAt,
                alert.lastCheckedAt,
                alert.lastCheckedAt == null
                        ? now.isAfter(alert.createdAt.plus(AvailabilityService.CHECK_INTERVAL.multipliedBy(3)))
                        : now.isAfter(alert.lastCheckedAt.plus(AvailabilityService.CHECK_INTERVAL.multipliedBy(3))),
                delivery);
    }

    private boolean hasExactSlot(List<SlotAvailability> slots, ApiModels.TimeSlotRequest requested) {
        LocalTime start = LocalTime.parse(requested.startTime());
        LocalTime end = LocalTime.parse(requested.endTime());
        return slots.stream().anyMatch(slot -> slot.startTime().equals(start) && slot.endTime().equals(end));
    }

    private boolean hasAvailableSlot(List<SlotAvailability> slots, LocalTime start, LocalTime end) {
        return slots.stream()
                .anyMatch(slot -> slot.startTime().equals(start)
                        && slot.endTime().equals(end)
                        && slot.available());
    }

    private Instant expiryAt(LocalDate date, LocalTime startTime) {
        return date.atTime(startTime).atZone(COURT_ZONE).toInstant();
    }

    private String fingerprint(ApiModels.AlertRequest request) {
        String value = request.courtId() + "|" + request.date() + "|" +
                request.slot().startTime() + "|" + request.slot().endTime();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }

    public record CreateResult(ApiModels.AlertResponse body, int status, boolean replayed) {
    }

    private record IdempotencyKey(String subject, String operation, String key) {
    }

    private record StoredResponse(String fingerprint, ApiModels.AlertResponse body,
                                  int status, Instant expiresAt) {
    }

    private enum AlertStatus {
        WATCHING, NOTIFIED, CANCELED, EXPIRED
    }

    private enum DeliveryStatus {
        PENDING, SENT, FAILED
    }

    private static final class DeliveryState {
        private final DeliveryStatus status;
        private final int attemptCount;
        private final Instant lastAttemptAt;
        private final String failureReason;

        private DeliveryState(DeliveryStatus status, int attemptCount,
                              Instant lastAttemptAt, String failureReason) {
            this.status = status;
            this.attemptCount = attemptCount;
            this.lastAttemptAt = lastAttemptAt;
            this.failureReason = failureReason;
        }
    }

    private static final class StoredAlert {
        private UUID alertId;
        private String subject;
        private String courtId;
        private String courtName;
        private String reservationUrl;
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private AlertStatus status;
        private Instant createdAt;
        private Instant expiresAt;
        private Instant lastCheckedAt;
        private DeliveryState delivery;
    }
}
