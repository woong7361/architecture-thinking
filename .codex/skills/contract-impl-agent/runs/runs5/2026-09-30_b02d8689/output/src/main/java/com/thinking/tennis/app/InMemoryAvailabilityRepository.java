package com.thinking.tennis.app;

import com.thinking.tennis.domain.AvailabilitySnapshot;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryAvailabilityRepository implements AvailabilityRepository {

    private final ConcurrentMap<String, AvailabilitySnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public Optional<AvailabilitySnapshot> find(String courtId, LocalDate date) {
        return Optional.ofNullable(snapshots.get(key(courtId, date)));
    }

    @Override
    public void save(AvailabilitySnapshot snapshot) {
        snapshots.put(key(snapshot.court().courtId(), snapshot.date()), snapshot);
    }

    private String key(String courtId, LocalDate date) {
        return courtId + "\u0000" + date;
    }
}
