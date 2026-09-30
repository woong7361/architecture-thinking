package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRepository {

    Alert save(Alert alert);

    Optional<Alert> findById(UUID alertId);

    List<Alert> findByOwner(String ownerId);

    Optional<Alert> findWatching(String ownerId,
                                 String courtId,
                                 LocalDate date,
                                 LocalTime startTime,
                                 LocalTime endTime);

    List<Alert> findByCourtAndDate(String courtId, LocalDate date);
}
