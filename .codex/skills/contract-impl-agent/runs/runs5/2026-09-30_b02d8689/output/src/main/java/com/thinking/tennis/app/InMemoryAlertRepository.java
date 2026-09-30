package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryAlertRepository implements AlertRepository {

    private final ConcurrentMap<UUID, Alert> alerts = new ConcurrentHashMap<>();

    @Override
    public Alert save(Alert alert) {
        alerts.put(alert.alertId(), alert);
        return alert;
    }

    @Override
    public Optional<Alert> findById(UUID alertId) {
        return Optional.ofNullable(alerts.get(alertId));
    }

    @Override
    public List<Alert> findByOwner(String ownerId) {
        return alerts.values().stream()
                .filter(alert -> alert.ownerId().equals(ownerId))
                .sorted(Comparator.comparing(Alert::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<Alert> findWatching(String ownerId,
                                        String courtId,
                                        LocalDate date,
                                        LocalTime startTime,
                                        LocalTime endTime) {
        return alerts.values().stream()
                .filter(alert -> alert.ownerId().equals(ownerId))
                .filter(alert -> alert.status().name().equals("WATCHING"))
                .filter(alert -> alert.matches(courtId, date, startTime, endTime))
                .findFirst();
    }

    @Override
    public List<Alert> findByCourtAndDate(String courtId, LocalDate date) {
        return new ArrayList<>(alerts.values().stream()
                .filter(alert -> alert.courtId().equals(courtId))
                .filter(alert -> alert.date().equals(date))
                .toList());
    }
}
