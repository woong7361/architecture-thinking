package com.thinking.tennis.app;

import com.thinking.tennis.domain.AvailabilitySnapshot;

import java.time.LocalDate;
import java.util.Optional;

public interface AvailabilityRepository {

    Optional<AvailabilitySnapshot> find(String courtId, LocalDate date);

    void save(AvailabilitySnapshot snapshot);
}
