package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.AvailabilitySnapshot;
import com.thinking.tennis.port.CourtAvailabilityPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public final class AlertApplication {

    public static final long AVAILABILITY_REFRESH_SECONDS = 20;
    public static final long ALERT_DELAY_THRESHOLD_SECONDS = AVAILABILITY_REFRESH_SECONDS * 3;
    public static final long IDEMPOTENCY_RETENTION_HOURS = 24;
    public static final ZoneId COURT_ZONE = ZoneId.of("Asia/Seoul");

    private final CourtAvailabilityPort courtAvailabilityPort;
    private final AlertRepository alertRepository;
    private final AvailabilityRepository availabilityRepository;
    private final Clock clock;
    private final Map<String, IdempotencyEntry> idempotencyEntries = new ConcurrentHashMap<>();
    private final Map<String, Object> refreshLocks = new ConcurrentHashMap<>();
    private final Map<String, Boolean> inFlightIdempotencyKeys = new ConcurrentHashMap<>();
    private final Map<String, Object> conditionLocks = new ConcurrentHashMap<>();
    private final Map<String, RefreshFailure> recentRefreshFailures = new ConcurrentHashMap<>();

    @Autowired
    public AlertApplication(CourtAvailabilityPort courtAvailabilityPort,
                            AlertRepository alertRepository,
                            AvailabilityRepository availabilityRepository) {
        this(courtAvailabilityPort, alertRepository, availabilityRepository, Clock.systemUTC());
    }

    public AlertApplication(CourtAvailabilityPort courtAvailabilityPort,
                            AlertRepository alertRepository,
                            AvailabilityRepository availabilityRepository,
                            Clock clock) {
        this.courtAvailabilityPort = courtAvailabilityPort;
        this.alertRepository = alertRepository;
        this.availabilityRepository = availabilityRepository;
        this.clock = clock;
    }

    public AvailabilityResult getCourtAvailability(String courtId, LocalDate date) {
        CourtAvailabilityPort.SupportedCourt court = courtAvailabilityPort.findCourt(courtId)
                .orElseThrow(() -> new CourtNotSupportedException(courtId));
        String cacheKey = cacheKey(courtId, date);
        Object refreshLock = refreshLocks.computeIfAbsent(cacheKey, ignored -> new Object());

        synchronized (refreshLock) {
            Instant now = clock.instant();
            Optional<AvailabilitySnapshot> previous = availabilityRepository.find(courtId, date);
            if (previous.isPresent() && isFresh(previous.get(), now)) {
                applySnapshotToAlerts(previous.get(), false, null, now);
                return new AvailabilityResult(previous.get(), false, null);
            }
            RefreshFailure recentFailure = recentRefreshFailures.get(cacheKey);
            if (recentFailure != null
                    && recentFailure.recordedAt().plusSeconds(AVAILABILITY_REFRESH_SECONDS).isAfter(now)) {
                if (recentFailure.snapshot() == null) {
                    throw new AvailabilityFailureException(recentFailure.reason(), false);
                }
                applySnapshotToAlerts(recentFailure.snapshot(), true, recentFailure.reason(), now);
                return new AvailabilityResult(recentFailure.snapshot(), true, recentFailure.reason());
            }

            try {
                CourtAvailabilityPort.CourtAvailabilityCheck checked =
                        courtAvailabilityPort.checkAvailability(courtId, date);
                AvailabilitySnapshot current = new AvailabilitySnapshot(
                        checked.court(),
                        checked.date(),
                        checked.confirmedAt(),
                        checked.slots()
                );
                availabilityRepository.save(current);
                recentRefreshFailures.remove(cacheKey);
                applySnapshotToAlerts(current, false, null, now);
                return new AvailabilityResult(current, false, null);
            } catch (CourtAvailabilityPort.CourtNotSupportedException exception) {
                throw new CourtNotSupportedException(exception.courtId());
            } catch (CourtAvailabilityPort.CheckFailed exception) {
                if (previous.isPresent()) {
                    recentRefreshFailures.put(cacheKey,
                            new RefreshFailure(previous.get(), exception.reason(), now));
                    applySnapshotToAlerts(previous.get(), true, exception.reason(), now);
                    return new AvailabilityResult(previous.get(), true, exception.reason());
                }
                recentRefreshFailures.put(cacheKey,
                        new RefreshFailure(null, exception.reason(), now));
                throw new AvailabilityFailureException(exception.reason(), false);
            }
        }
    }

    public CreateAlertResult createAlert(String ownerId,
                                         UUID idempotencyKey,
                                         String courtId,
                                         LocalDate date,
                                         LocalTime startTime,
                                         LocalTime endTime) {
        String idempotencyScope = ownerId + "\u0000createAlert\u0000" + idempotencyKey;
        Instant now = clock.instant();
        IdempotencyEntry existing = idempotencyEntries.get(idempotencyScope);
        if (existing != null && existing.expiresAt().isAfter(now)) {
            if (!existing.matches(courtId, date, startTime, endTime)) {
                throw new IdempotencyKeyReusedException();
            }
            return new CreateAlertResult(existing.alert().copy(), existing.status(), true);
        }
        if (existing != null) {
            idempotencyEntries.remove(idempotencyScope, existing);
        }

        if (inFlightIdempotencyKeys.putIfAbsent(idempotencyScope, Boolean.TRUE) != null) {
            throw new ConcurrentUpdateConflictException();
        }
        try {
            CourtAvailabilityPort.SupportedCourt court = courtAvailabilityPort.findCourt(courtId)
                    .orElseThrow(() -> new CourtNotSupportedException(courtId));
            validateSlot(courtId, date, startTime, endTime);
            Instant expiresAt = date.atTime(startTime).atZone(COURT_ZONE).toInstant();
            if (!expiresAt.isAfter(now)) {
                throw new AlertWindowClosedException();
            }

            String conditionKey = ownerId + "\u0000" + courtId + "\u0000" + date
                    + "\u0000" + startTime + "\u0000" + endTime;
            Object conditionLock = conditionLocks.computeIfAbsent(conditionKey, ignored -> new Object());
            synchronized (conditionLock) {
                Optional<Alert> active = alertRepository.findWatching(
                        ownerId, courtId, date, startTime, endTime
                );
                if (active.isPresent()) {
                    IdempotencyEntry entry = new IdempotencyEntry(
                            active.get().copy(),
                            200,
                            courtId,
                            date,
                            startTime,
                            endTime,
                            now.plus(Duration.ofHours(IDEMPOTENCY_RETENTION_HOURS))
                    );
                    idempotencyEntries.put(idempotencyScope, entry);
                    return new CreateAlertResult(active.get(), 200, false);
                }

                Alert alert = new Alert(
                        UUID.randomUUID(),
                        ownerId,
                        court.courtId(),
                        court.courtName(),
                        court.reservationUrl(),
                        date,
                        startTime,
                        endTime,
                        now,
                        expiresAt
                );
                alertRepository.save(alert);
                IdempotencyEntry entry = new IdempotencyEntry(
                        alert.copy(),
                        201,
                        courtId,
                        date,
                        startTime,
                        endTime,
                        now.plus(Duration.ofHours(IDEMPOTENCY_RETENTION_HOURS))
                );
                idempotencyEntries.put(idempotencyScope, entry);
                return new CreateAlertResult(alert, 201, false);
            }
        } finally {
            inFlightIdempotencyKeys.remove(idempotencyScope);
        }
    }

    public List<Alert> listAlerts(String ownerId, AlertStatus status) {
        return alertRepository.findByOwner(ownerId).stream()
                .filter(alert -> status == null || alert.status() == status)
                .peek(alert -> refreshDerivedState(alert, clock.instant()))
                .toList();
    }

    public Alert getAlert(String ownerId, UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
                .filter(candidate -> candidate.ownerId().equals(ownerId))
                .orElseThrow(AlertNotFoundException::new);
        refreshDerivedState(alert, clock.instant());
        return alert;
    }

    public Alert cancelAlert(String ownerId, UUID alertId) {
        Alert alert = getAlert(ownerId, alertId);
        if (alert.status() == AlertStatus.WATCHING) {
            alert.cancel();
            alertRepository.save(alert);
        }
        return alert;
    }

    private void validateSlot(String courtId,
                              LocalDate date,
                              LocalTime startTime,
                              LocalTime endTime) {
        AvailabilityResult availability;
        try {
            availability = getCourtAvailability(courtId, date);
        } catch (AvailabilityFailureException exception) {
            throw new AlertApplicationException("신청 조건을 확인하지 못했습니다", exception);
        }
        boolean supported = availability.snapshot().slots().stream()
                .anyMatch(slot -> slot.startTime().equals(startTime)
                        && slot.endTime().equals(endTime));
        if (!supported) {
            throw new SlotNotSupportedException(
                    courtId + "은 그 날짜에 " + startTime + "~" + endTime + " 단위로 운영합니다"
            );
        }
    }

    private void applySnapshotToAlerts(AvailabilitySnapshot snapshot,
                                       boolean stale,
                                       CourtAvailabilityPort.UpstreamFailureReason failureReason,
                                       Instant now) {
        for (Alert alert : alertRepository.findByCourtAndDate(
                snapshot.court().courtId(), snapshot.date())) {
            refreshDerivedState(alert, now);
            if (!stale) {
                alert.markChecked(snapshot.confirmedAt(),
                        isDelayed(snapshot.confirmedAt(), now));
                snapshot.slots().stream()
                        .filter(slot -> slot.startTime().equals(alert.startTime()))
                        .filter(slot -> slot.endTime().equals(alert.endTime()))
                        .findFirst()
                        .ifPresent(slot -> {
                            if (slot.available()) {
                                if (alert.status() == AlertStatus.WATCHING
                                        && !alert.hasTerminalDeliveryFailure()) {
                                    alert.queueDelivery();
                                }
                            } else {
                                alert.clearPendingDelivery();
                            }
                        });
                alertRepository.save(alert);
            } else {
                alert.markDelayed(isDelayed(
                        alert.lastCheckedAt() == null ? alert.createdAt() : alert.lastCheckedAt(),
                        now
                ));
                alertRepository.save(alert);
            }
        }
    }

    private void refreshDerivedState(Alert alert, Instant now) {
        if (alert.status() == AlertStatus.WATCHING && !alert.expiresAt().isAfter(now)) {
            alert.expire();
            alertRepository.save(alert);
        }
        Instant reference = alert.lastCheckedAt() == null ? alert.createdAt() : alert.lastCheckedAt();
        alert.markDelayed(isDelayed(reference, now));
    }

    private boolean isFresh(AvailabilitySnapshot snapshot, Instant now) {
        return !snapshot.confirmedAt().plusSeconds(AVAILABILITY_REFRESH_SECONDS).isBefore(now);
    }

    private boolean isDelayed(Instant reference, Instant now) {
        return reference.plusSeconds(ALERT_DELAY_THRESHOLD_SECONDS).isBefore(now);
    }

    private String cacheKey(String courtId, LocalDate date) {
        return courtId + "\u0000" + date;
    }

    private record IdempotencyEntry(
            Alert alert,
            int status,
            String courtId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Instant expiresAt
    ) {
        private boolean matches(String courtId,
                                LocalDate date,
                                LocalTime startTime,
                                LocalTime endTime) {
            return this.courtId.equals(courtId)
                    && this.date.equals(date)
                    && this.startTime.equals(startTime)
                    && this.endTime.equals(endTime);
        }
    }

    private record RefreshFailure(
            AvailabilitySnapshot snapshot,
            CourtAvailabilityPort.UpstreamFailureReason reason,
            Instant recordedAt
    ) {
    }
}
