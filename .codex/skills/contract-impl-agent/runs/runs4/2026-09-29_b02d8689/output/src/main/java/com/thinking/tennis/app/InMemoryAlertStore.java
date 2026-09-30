package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertStatus;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryAlertStore {

    private static final Duration IDEMPOTENCY_RETENTION = Duration.ofHours(24);

    private final Map<UUID, Alert> alerts = new ConcurrentHashMap<>();
    private final Map<IdempotencyKey, IdempotencyEntry> idempotency = new ConcurrentHashMap<>();

    public synchronized IdempotencyLookup beginIdempotentRequest(
            String ownerId,
            String operation,
            String key,
            String fingerprint,
            Instant now) {
        purgeExpiredIdempotency(now);
        IdempotencyKey idempotencyKey = new IdempotencyKey(ownerId, operation, key);
        IdempotencyEntry existing = idempotency.get(idempotencyKey);
        if (existing == null) {
            idempotency.put(idempotencyKey, IdempotencyEntry.inFlight(fingerprint, now));
            return IdempotencyLookup.newRequest();
        }
        if (!existing.fingerprint().equals(fingerprint)) {
            return IdempotencyLookup.reused();
        }
        if (existing.completedAlert() == null) {
            return IdempotencyLookup.inFlight();
        }
        return IdempotencyLookup.replayed(existing.completedAlert());
    }

    public synchronized void completeIdempotentRequest(
            String ownerId,
            String operation,
            String key,
            Alert alert,
            Instant now) {
        idempotency.put(new IdempotencyKey(ownerId, operation, key),
                new IdempotencyEntry(
                        idempotency.get(new IdempotencyKey(ownerId, operation, key)).fingerprint(),
                        now,
                        alert));
    }

    public synchronized void abandonIdempotentRequest(
            String ownerId,
            String operation,
            String key) {
        idempotency.remove(new IdempotencyKey(ownerId, operation, key));
    }

    public synchronized Alert findActiveByCondition(
            String ownerId,
            String courtId,
            java.time.LocalDate date,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime) {
        return alerts.values().stream()
                .filter(alert -> alert.ownerId().equals(ownerId))
                .filter(alert -> alert.status() == AlertStatus.WATCHING)
                .filter(alert -> alert.courtId().equals(courtId))
                .filter(alert -> alert.date().equals(date))
                .filter(alert -> alert.startTime().equals(startTime))
                .filter(alert -> alert.endTime().equals(endTime))
                .findFirst()
                .orElse(null);
    }

    public synchronized void save(Alert alert) {
        alerts.put(alert.alertId(), alert);
    }

    public synchronized Alert find(UUID alertId) {
        return alerts.get(alertId);
    }

    public synchronized List<Alert> findByOwner(String ownerId, AlertStatus status) {
        return alerts.values().stream()
                .filter(alert -> alert.ownerId().equals(ownerId))
                .filter(alert -> status == null || alert.status() == status)
                .sorted(Comparator.comparing(Alert::createdAt).reversed())
                .toList();
    }

    public synchronized List<Alert> expireBefore(Instant now) {
        List<Alert> expired = new ArrayList<>();
        for (Alert alert : alerts.values()) {
            if (alert.status() == AlertStatus.WATCHING && !alert.expiresAt().isAfter(now)) {
                Alert next = alert.withStatus(AlertStatus.EXPIRED);
                alerts.put(alert.alertId(), next);
                expired.add(next);
            }
        }
        return expired;
    }

    private void purgeExpiredIdempotency(Instant now) {
        idempotency.entrySet().removeIf(entry ->
                Duration.between(entry.getValue().createdAt(), now).compareTo(IDEMPOTENCY_RETENTION) > 0);
    }

    public record IdempotencyLookup(Kind kind, Alert alert) {
        public enum Kind {
            NEW,
            REPLAYED,
            IN_FLIGHT,
            REUSED
        }

        public static IdempotencyLookup newRequest() {
            return new IdempotencyLookup(Kind.NEW, null);
        }

        public static IdempotencyLookup replayed(Alert alert) {
            return new IdempotencyLookup(Kind.REPLAYED, alert);
        }

        public static IdempotencyLookup inFlight() {
            return new IdempotencyLookup(Kind.IN_FLIGHT, null);
        }

        public static IdempotencyLookup reused() {
            return new IdempotencyLookup(Kind.REUSED, null);
        }
    }

    private record IdempotencyKey(String ownerId, String operation, String key) {
    }

    private record IdempotencyEntry(String fingerprint, Instant createdAt, Alert completedAlert) {
        static IdempotencyEntry inFlight(String fingerprint, Instant createdAt) {
            return new IdempotencyEntry(fingerprint, createdAt, null);
        }
    }
}
