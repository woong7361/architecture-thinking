package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.TimeSlot;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryAlertStore {

    private final ConcurrentHashMap<UUID, Alert> alerts = new ConcurrentHashMap<>();

    public Alert save(Alert alert) {
        alerts.put(alert.alertId(), alert);
        return alert;
    }

    public Optional<Alert> findForUser(String userId, UUID alertId) {
        Alert alert = alerts.get(alertId);
        if (alert == null || !alert.userId().equals(userId)) {
            return Optional.empty();
        }
        return Optional.of(alert);
    }

    public List<Alert> listForUser(String userId, AlertStatus status) {
        return alerts.values().stream()
                .filter(alert -> alert.userId().equals(userId))
                .filter(alert -> status == null || alert.status() == status)
                .sorted(Comparator.comparing(Alert::createdAt).reversed())
                .toList();
    }

    public Optional<Alert> findWatchingByCondition(String userId, String courtId, LocalDate date, TimeSlot slot) {
        return alerts.values().stream()
                .filter(alert -> alert.userId().equals(userId))
                .filter(Alert::isWatching)
                .filter(alert -> alert.courtId().equals(courtId))
                .filter(alert -> alert.date().equals(date))
                .filter(alert -> alert.slot().equals(slot))
                .findFirst();
    }

    public List<Alert> listWatchingByCourtDate(String courtId, LocalDate date) {
        return alerts.values().stream()
                .filter(Alert::isWatching)
                .filter(alert -> alert.courtId().equals(courtId))
                .filter(alert -> alert.date().equals(date))
                .toList();
    }

    public List<Alert> snapshot() {
        return new ArrayList<>(alerts.values());
    }

    public void expireDue(Instant now) {
        alerts.values().forEach(alert -> {
            if (alert.isWatching() && !alert.expiresAt().isAfter(now)) {
                alert.markExpired();
            }
        });
    }
}
