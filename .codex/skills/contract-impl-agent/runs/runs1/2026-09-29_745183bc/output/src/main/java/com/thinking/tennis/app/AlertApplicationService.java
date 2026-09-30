package com.thinking.tennis.app;

import com.thinking.tennis.port.CourtAvailabilityPort;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class AlertApplicationService {

    public static final ZoneId COURT_ZONE = ZoneId.of("Asia/Seoul");
    public static final int CHECK_INTERVAL_SECONDS = 20;
    private static final int CHECK_DELAY_MULTIPLIER = 3;
    private static final long IDEMPOTENCY_RETENTION_SECONDS = 24L * 60L * 60L;

    private final AvailabilityApplicationService availabilityService;
    private final CourtAvailabilityPort courtAvailabilityPort;
    private final Clock clock;
    private final Object lock = new Object();
    private final Map<UUID, StoredAlert> alerts = new HashMap<>();
    private final Map<IdempotencyScope, IdempotencyRecord> idempotencyRecords = new HashMap<>();

    public AlertApplicationService(AvailabilityApplicationService availabilityService,
                                   CourtAvailabilityPort courtAvailabilityPort,
                                   Clock clock) {
        this.availabilityService = availabilityService;
        this.courtAvailabilityPort = courtAvailabilityPort;
        this.clock = clock;
    }

    public CreateAlertResult createAlert(String userId, UUID idempotencyKey, CreateAlertCommand command) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(command, "command");

        String fingerprint = command.fingerprint();
        IdempotencyScope scope = new IdempotencyScope(userId, "createAlert", idempotencyKey);
        Instant now = clock.instant();
        synchronized (lock) {
            pruneIdempotency(now);
            IdempotencyRecord existing = idempotencyRecords.get(scope);
            if (existing != null) {
                if (existing.inProgress()) {
                    throw AppFailure.concurrentUpdate();
                }
                if (!existing.fingerprint().equals(fingerprint)) {
                    throw AppFailure.of(FailureCode.IDEMPOTENCY_KEY_REUSED);
                }
                return new CreateAlertResult(existing.status(), existing.alert(), true);
            }
            idempotencyRecords.put(scope, IdempotencyRecord.inProgress(fingerprint, now));
        }

        try {
            CreateAlertResult result = createAlertWithoutIdempotency(userId, command, now);
            synchronized (lock) {
                idempotencyRecords.put(scope, IdempotencyRecord.completed(fingerprint, now, result.status(), result.alert()));
            }
            return result;
        } catch (RuntimeException failure) {
            synchronized (lock) {
                idempotencyRecords.remove(scope);
            }
            throw failure;
        }
    }

    public AlertList listAlerts(String userId, Optional<AlertStatus> status) {
        Objects.requireNonNull(userId, "userId");
        Instant now = clock.instant();
        List<AlertView> items;
        synchronized (lock) {
            items = alerts.values().stream()
                    .filter(alert -> alert.userId().equals(userId))
                    .filter(alert -> status.isEmpty() || alert.status() == status.get())
                    .sorted(Comparator.comparing(StoredAlert::createdAt).reversed())
                    .map(alert -> viewOf(alert, now))
                    .toList();
        }
        return new AlertList(items);
    }

    public AlertView getAlert(String userId, UUID alertId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(alertId, "alertId");
        synchronized (lock) {
            StoredAlert alert = alerts.get(alertId);
            if (alert == null || !alert.userId().equals(userId)) {
                throw AppFailure.of(FailureCode.ALERT_NOT_FOUND);
            }
            return viewOf(alert, clock.instant());
        }
    }

    public AlertView cancelAlert(String userId, UUID alertId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(alertId, "alertId");
        synchronized (lock) {
            StoredAlert alert = alerts.get(alertId);
            if (alert == null || !alert.userId().equals(userId)) {
                throw AppFailure.of(FailureCode.ALERT_NOT_FOUND);
            }
            if (alert.status() != AlertStatus.WATCHING) {
                return viewOf(alert, clock.instant());
            }
            StoredAlert canceled = alert.cancel();
            alerts.put(alertId, canceled);
            return viewOf(canceled, clock.instant());
        }
    }

    private CreateAlertResult createAlertWithoutIdempotency(String userId, CreateAlertCommand command, Instant now) {
        if (command.courtId().length() > 64 || !command.courtId().matches("^[a-z0-9][a-z0-9-]*$")) {
            throw AppFailure.of(FailureCode.VALIDATION_FAILED);
        }
        LocalDate date = parseDate(command.date());
        TimeSlot slot = parseSlot(command.slot());
        Instant expiresAt = date.atTime(slot.startTime()).atZone(COURT_ZONE).toInstant();
        if (!expiresAt.isAfter(now)) {
            throw AppFailure.of(FailureCode.ALERT_WINDOW_CLOSED);
        }
        synchronized (lock) {
            Optional<StoredAlert> existing = findWatchingAlert(userId, command, slot);
            if (existing.isPresent()) {
                return new CreateAlertResult(200, viewOf(existing.get(), now), false);
            }
        }
        CourtAvailabilityPort.SupportedCourt court = courtAvailabilityPort.findCourt(command.courtId())
                .orElseThrow(() -> AppFailure.of(FailureCode.COURT_NOT_SUPPORTED));
        ensureSlotSupported(command.courtId(), date, slot);

        synchronized (lock) {
            Optional<StoredAlert> existing = findWatchingAlert(userId, command, slot);
            if (existing.isPresent()) {
                return new CreateAlertResult(200, viewOf(existing.get(), now), false);
            }

            UUID alertId = UUID.randomUUID();
            StoredAlert created = new StoredAlert(alertId, userId, court.courtId(), court.courtName(), court.reservationUrl(),
                    command.date(), slot, AlertStatus.WATCHING, now, expiresAt, null, null);
            alerts.put(alertId, created);
            return new CreateAlertResult(201, viewOf(created, now), false);
        }
    }

    private Optional<StoredAlert> findWatchingAlert(String userId, CreateAlertCommand command, TimeSlot slot) {
        return alerts.values().stream()
                .filter(alert -> alert.userId().equals(userId))
                .filter(alert -> alert.status() == AlertStatus.WATCHING)
                .filter(alert -> alert.courtId().equals(command.courtId()))
                .filter(alert -> alert.date().equals(command.date()))
                .filter(alert -> alert.slot().equals(slot))
                .findFirst();
    }

    private void ensureSlotSupported(String courtId, LocalDate date, TimeSlot slot) {
        AvailabilityApplicationService.CourtAvailabilityView availability = availabilityService.getAvailability(courtId, date);
        boolean supported = availability.slots().stream()
                .anyMatch(candidate -> candidate.startTime().equals(formatTime(slot.startTime()))
                        && candidate.endTime().equals(formatTime(slot.endTime())));
        if (!supported) {
            throw AppFailure.of(FailureCode.SLOT_NOT_SUPPORTED);
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException failure) {
            throw AppFailure.of(FailureCode.VALIDATION_FAILED);
        }
    }

    private TimeSlot parseSlot(TimeSlotInput input) {
        if (input == null || input.startTime() == null || input.endTime() == null) {
            throw AppFailure.of(FailureCode.VALIDATION_FAILED);
        }
        if (!input.startTime().matches("^([01][0-9]|2[0-3]):[0-5][0-9]$")
                || !input.endTime().matches("^([01][0-9]|2[0-3]):[0-5][0-9]$")) {
            throw AppFailure.of(FailureCode.VALIDATION_FAILED);
        }
        try {
            return new TimeSlot(LocalTime.parse(input.startTime(), DateTimeFormatter.ofPattern("HH:mm")),
                    LocalTime.parse(input.endTime(), DateTimeFormatter.ofPattern("HH:mm")));
        } catch (DateTimeParseException failure) {
            throw AppFailure.of(FailureCode.VALIDATION_FAILED);
        }
    }

    private void pruneIdempotency(Instant now) {
        List<IdempotencyScope> expired = new ArrayList<>();
        for (Map.Entry<IdempotencyScope, IdempotencyRecord> entry : idempotencyRecords.entrySet()) {
            if (!entry.getValue().inProgress()
                    && entry.getValue().createdAt().plusSeconds(IDEMPOTENCY_RETENTION_SECONDS).isBefore(now)) {
                expired.add(entry.getKey());
            }
        }
        expired.forEach(idempotencyRecords::remove);
    }

    private static String formatInstant(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    public static String formatTime(LocalTime time) {
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private AlertView viewOf(StoredAlert alert, Instant now) {
        Instant availabilityCheckedAt = null;
        try {
            availabilityCheckedAt = availabilityService.lastSuccessfulAt(alert.courtId(), LocalDate.parse(alert.date()));
        } catch (DateTimeParseException ignored) {
            // The request validator prevents this state; retaining the stored value is the least surprising fallback.
        }
        return alert.withComputedFields(now, availabilityCheckedAt);
    }

    public record CreateAlertCommand(String courtId, String date, TimeSlotInput slot) {
        public CreateAlertCommand {
            if (courtId == null || date == null || slot == null) {
                throw AppFailure.of(FailureCode.VALIDATION_FAILED);
            }
        }

        private String fingerprint() {
            return courtId + "|" + date + "|" + slot.startTime() + "|" + slot.endTime();
        }
    }

    public record TimeSlotInput(String startTime, String endTime) {
    }

    public record CreateAlertResult(int status, AlertView alert, boolean idempotencyReplayed) {
    }

    public record AlertList(List<AlertView> items) {
    }

    public record AlertView(String alertId,
                            String courtId,
                            String courtName,
                            String reservationUrl,
                            String date,
                            TimeSlotView slot,
                            AlertStatus status,
                            String createdAt,
                            String expiresAt,
                            String lastCheckedAt,
                            boolean checkDelayed,
                            AlertDeliveryView delivery) {
    }

    public record TimeSlotView(String startTime, String endTime) {
    }

    public record AlertDeliveryView(DeliveryStatus status,
                                    int attemptCount,
                                    String lastAttemptAt,
                                    String failureReason) {
    }

    private record TimeSlot(LocalTime startTime, LocalTime endTime) {
        private TimeSlotView toView() {
            return new TimeSlotView(formatTime(startTime), formatTime(endTime));
        }
    }

    private record StoredAlert(UUID alertId,
                               String userId,
                               String courtId,
                               String courtName,
                               String reservationUrl,
                               String date,
                               TimeSlot slot,
                               AlertStatus status,
                               Instant createdAt,
                               Instant expiresAt,
                               Instant lastCheckedAt,
                               AlertDeliveryView delivery) {
        private StoredAlert cancel() {
            AlertDeliveryView keptDelivery = delivery != null && delivery.status() == DeliveryStatus.PENDING ? null : delivery;
            return new StoredAlert(alertId, userId, courtId, courtName, reservationUrl, date, slot, AlertStatus.CANCELED,
                    createdAt, expiresAt, lastCheckedAt, keptDelivery);
        }

        private AlertView withComputedFields(Instant now, Instant availabilityCheckedAt) {
            Instant effectiveLastCheckedAt = lastCheckedAt;
            if (availabilityCheckedAt != null
                    && (effectiveLastCheckedAt == null || availabilityCheckedAt.isAfter(effectiveLastCheckedAt))) {
                effectiveLastCheckedAt = availabilityCheckedAt;
            }
            Instant checkBase = effectiveLastCheckedAt == null ? createdAt : effectiveLastCheckedAt;
            boolean checkDelayed = checkBase.plusSeconds((long) CHECK_INTERVAL_SECONDS * CHECK_DELAY_MULTIPLIER).isBefore(now);
            return new AlertView(alertId.toString(), courtId, courtName, reservationUrl, date, slot.toView(), status,
                    formatInstant(createdAt), formatInstant(expiresAt), formatInstant(effectiveLastCheckedAt), checkDelayed, delivery);
        }
    }

    private record IdempotencyScope(String userId, String operation, UUID key) {
    }

    private record IdempotencyRecord(String fingerprint,
                                     Instant createdAt,
                                     boolean inProgress,
                                     int status,
                                     AlertView alert) {
        private static IdempotencyRecord inProgress(String fingerprint, Instant createdAt) {
            return new IdempotencyRecord(fingerprint, createdAt, true, 0, null);
        }

        private static IdempotencyRecord completed(String fingerprint, Instant createdAt, int status, AlertView alert) {
            return new IdempotencyRecord(fingerprint, createdAt, false, status, alert);
        }
    }

    public enum AlertStatus {
        WATCHING,
        NOTIFIED,
        CANCELED,
        EXPIRED
    }

    public enum DeliveryStatus {
        PENDING,
        SENT,
        FAILED
    }

    public enum FailureCode {
        VALIDATION_FAILED,
        UNAUTHENTICATED,
        COURT_NOT_SUPPORTED,
        SLOT_NOT_SUPPORTED,
        ALERT_WINDOW_CLOSED,
        ALERT_NOT_FOUND,
        IDEMPOTENCY_KEY_REUSED,
        UPSTREAM_TIMEOUT,
        UPSTREAM_UNAVAILABLE,
        UPSTREAM_RESPONSE_UNREADABLE,
        STORAGE_TIMEOUT,
        CONCURRENT_UPDATE_CONFLICT,
        INTERNAL_ERROR
    }

    public static final class AppFailure extends RuntimeException {
        private final FailureCode code;
        private final Integer retryAfterSeconds;

        private AppFailure(FailureCode code, Integer retryAfterSeconds) {
            super(code.name());
            this.code = code;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public static AppFailure of(FailureCode code) {
            return new AppFailure(code, null);
        }

        public static AppFailure retryable(FailureCode code, int retryAfterSeconds) {
            return new AppFailure(code, retryAfterSeconds);
        }

        public static AppFailure concurrentUpdate() {
            return retryable(FailureCode.CONCURRENT_UPDATE_CONFLICT, 1);
        }

        public FailureCode code() {
            return code;
        }

        public Optional<Integer> retryAfterSeconds() {
            return Optional.ofNullable(retryAfterSeconds);
        }
    }
}
