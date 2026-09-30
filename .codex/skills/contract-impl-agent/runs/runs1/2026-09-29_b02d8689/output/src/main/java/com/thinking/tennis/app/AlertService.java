package com.thinking.tennis.app;


import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.Alert.AlertDelivery;
import com.thinking.tennis.domain.Alert.AlertStatus;
import com.thinking.tennis.domain.Alert.DeliveryStatus;
import com.thinking.tennis.domain.AlertRepository;
import com.thinking.tennis.domain.AvailabilitySnapshot;
import com.thinking.tennis.domain.DomainExceptions.AlertNotFound;
import com.thinking.tennis.domain.DomainExceptions.AlertWindowClosed;
import com.thinking.tennis.domain.DomainExceptions.ConcurrentUpdateConflict;
import com.thinking.tennis.domain.DomainExceptions.CourtNotSupported;
import com.thinking.tennis.domain.DomainExceptions.IdempotencyKeyReused;
import com.thinking.tennis.domain.DomainExceptions.SlotNotSupported;
import com.thinking.tennis.domain.TimeWindow;
import com.thinking.tennis.domain.UserId;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AlertService {

    private static final String CREATE_ALERT_OPERATION = "createAlert";
    private static final Duration IDEMPOTENCY_RETENTION = Duration.ofHours(24);
    private static final Duration CHECK_DELAY_THRESHOLD = AvailabilityService.CHECK_INTERVAL.multipliedBy(3);
    private static final ZoneId COURT_ZONE = ZoneId.of("Asia/Seoul");

    private final AlertRepository alertRepository;
    private final AvailabilityService availabilityService;
    private final Clock clock;
    private final ConcurrentMap<IdempotencyKey, IdempotencyRecord> completedIdempotency = new ConcurrentHashMap<>();
    private final Set<IdempotencyKey> processingIdempotency = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<ConditionKey, Object> conditionLocks = new ConcurrentHashMap<>();

    public AlertService(AlertRepository alertRepository, AvailabilityService availabilityService) {
        this(alertRepository, availabilityService, Clock.systemUTC());
    }

    AlertService(AlertRepository alertRepository, AvailabilityService availabilityService, Clock clock) {
        this.alertRepository = alertRepository;
        this.availabilityService = availabilityService;
        this.clock = clock;
    }

    public CreateAlertResult createAlert(UserId owner, UUID idempotencyKey, CreateAlertCommand command) {
        Instant now = clock.instant();
        expireCompletedIdempotency(now);
        expireDueAlerts(owner, now);

        IdempotencyKey key = new IdempotencyKey(owner, CREATE_ALERT_OPERATION, idempotencyKey);
        String fingerprint = command.fingerprint();
        IdempotencyRecord completed = completedIdempotency.get(key);
        if (completed != null) {
            if (!completed.fingerprint().equals(fingerprint)) {
                throw new IdempotencyKeyReused();
            }
            return new CreateAlertResult(completed.alert(), completed.created(), true);
        }

        if (!processingIdempotency.add(key)) {
            throw new ConcurrentUpdateConflict();
        }
        try {
            CreateAlertResult result = createAlertWithoutIdempotency(owner, command, now);
            Instant completedAt = clock.instant();
            completedIdempotency.put(key, new IdempotencyRecord(fingerprint, result.alert(), result.created(),
                    completedAt.plus(IDEMPOTENCY_RETENTION)));
            return result;
        } finally {
            processingIdempotency.remove(key);
        }
    }

    public List<Alert> listAlerts(UserId owner, Optional<AlertStatus> status) {
        Instant now = clock.instant();
        expireDueAlerts(owner, now);
        return alertRepository.findByOwner(owner).stream()
                .filter(alert -> status.isEmpty() || alert.status() == status.get())
                .map(alert -> withDerivedFields(alert, now))
                .toList();
    }

    public Alert getAlert(UserId owner, UUID alertId) {
        Instant now = clock.instant();
        expireDueAlerts(owner, now);
        Alert alert = alertRepository.findById(alertId)
                .filter(candidate -> candidate.owner().equals(owner))
                .orElseThrow(AlertNotFound::new);
        return withDerivedFields(alert, now);
    }

    public Alert cancelAlert(UserId owner, UUID alertId) {
        Instant now = clock.instant();
        expireDueAlerts(owner, now);
        Alert alert = alertRepository.findById(alertId)
                .filter(candidate -> candidate.owner().equals(owner))
                .orElseThrow(AlertNotFound::new);
        if (alert.status() == AlertStatus.WATCHING) {
            AlertDelivery delivery = alert.delivery();
            if (delivery != null && delivery.status() == DeliveryStatus.PENDING) {
                delivery = null;
            }
            alert = alert.withStatus(AlertStatus.CANCELED).withDelivery(delivery);
            alertRepository.save(alert);
        }
        return withDerivedFields(alert, now);
    }

    private CreateAlertResult createAlertWithoutIdempotency(UserId owner, CreateAlertCommand command, Instant now) {
        if (availabilityService.findCourt(command.courtId()).isEmpty()) {
            throw new CourtNotSupported(command.courtId());
        }

        AvailabilitySnapshot availability;
        try {
            availability = availabilityService.getCourtAvailability(command.courtId(), command.date());
        } catch (AvailabilityService.CourtNotSupported failure) {
            throw new CourtNotSupported(failure.courtId());
        } catch (AvailabilityService.UpstreamAvailabilityFailure failure) {
            throw new IllegalStateException("availability check failed while validating alert request", failure);
        }

        if (!availability.supports(command.slot())) {
            throw new SlotNotSupported();
        }

        Instant expiresAt = command.date().atTime(command.slot().startTime()).atZone(COURT_ZONE).toInstant();
        if (!expiresAt.isAfter(now)) {
            throw new AlertWindowClosed();
        }

        ConditionKey conditionKey = new ConditionKey(owner, command.courtId(), command.date(), command.slot());
        Object conditionLock = conditionLocks.computeIfAbsent(conditionKey, ignored -> new Object());
        synchronized (conditionLock) {
            Optional<Alert> existing = alertRepository.findWatchingByCondition(owner, command.courtId(),
                    command.date(), command.slot());
            if (existing.isPresent()) {
                return new CreateAlertResult(withDerivedFields(existing.get(), now), false, false);
            }

            Alert alert = new Alert(UUID.randomUUID(), owner, availability.courtId(), availability.courtName(),
                    availability.reservationUrl(), availability.date(), command.slot(), AlertStatus.WATCHING,
                    now, expiresAt, availability.confirmedAt(), false, null);
            alertRepository.save(alert);
            return new CreateAlertResult(withDerivedFields(alert, now), true, false);
        }
    }

    private void expireDueAlerts(UserId owner, Instant now) {
        for (Alert alert : alertRepository.findByOwner(owner)) {
            if (alert.status() == AlertStatus.WATCHING && !alert.expiresAt().isAfter(now)) {
                AlertDelivery delivery = alert.delivery();
                if (delivery != null && delivery.status() == DeliveryStatus.PENDING) {
                    delivery = null;
                }
                alertRepository.save(alert.withStatus(AlertStatus.EXPIRED).withDelivery(delivery));
            }
        }
    }

    private Alert withDerivedFields(Alert alert, Instant now) {
        if (alert.status() != AlertStatus.WATCHING) {
            return alert.withCheckDelayed(false);
        }
        Instant basis = alert.lastCheckedAt() == null ? alert.createdAt() : alert.lastCheckedAt();
        boolean delayed = Duration.between(basis, now).compareTo(CHECK_DELAY_THRESHOLD) > 0;
        return alert.withCheckDelayed(delayed);
    }

    private void expireCompletedIdempotency(Instant now) {
        completedIdempotency.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    public record CreateAlertCommand(String courtId, LocalDate date, TimeWindow slot) {

        String fingerprint() {
            return courtId + "|" + date + "|" + slot.startTime() + "|" + slot.endTime();
        }
    }

    public record CreateAlertResult(Alert alert, boolean created, boolean replayed) {
    }

    private record IdempotencyKey(UserId owner, String operation, UUID idempotencyKey) {
    }

    private record ConditionKey(UserId owner, String courtId, LocalDate date, TimeWindow slot) {
    }

    private record IdempotencyRecord(String fingerprint, Alert alert, boolean created, Instant expiresAt) {

        private IdempotencyRecord {
            Objects.requireNonNull(fingerprint);
            Objects.requireNonNull(alert);
        }
    }
}
