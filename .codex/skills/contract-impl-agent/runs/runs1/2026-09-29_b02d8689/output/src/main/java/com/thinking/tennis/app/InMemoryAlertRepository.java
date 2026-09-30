package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.AlertRepository;
import com.thinking.tennis.domain.TimeWindow;
import com.thinking.tennis.domain.UserId;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
class InMemoryAlertRepository implements AlertRepository {

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
    public List<Alert> findByOwner(UserId owner) {
        return ordered(alerts.values().stream()
                .filter(alert -> alert.owner().equals(owner))
                .toList());
    }

    @Override
    public Optional<Alert> findWatchingByCondition(UserId owner, String courtId, LocalDate date, TimeWindow slot) {
        return alerts.values().stream()
                .filter(Alert::isWatching)
                .filter(alert -> alert.owner().equals(owner))
                .filter(alert -> alert.courtId().equals(courtId))
                .filter(alert -> alert.date().equals(date))
                .filter(alert -> alert.slot().equals(slot))
                .findFirst();
    }

    @Override
    public void markChecked(String courtId, LocalDate date, Instant checkedAt) {
        alerts.replaceAll((ignored, alert) -> shouldMark(alert, courtId, date)
                ? alert.withLastCheckedAt(checkedAt)
                : alert);
    }

    private boolean shouldMark(Alert alert, String courtId, LocalDate date) {
        return alert.isWatching()
                && alert.courtId().equals(courtId)
                && alert.date().equals(date);
    }

    private List<Alert> ordered(List<Alert> source) {
        List<Alert> copy = new ArrayList<>(source);
        copy.sort(Comparator.comparing(Alert::createdAt).reversed());
        return copy;
    }
}
