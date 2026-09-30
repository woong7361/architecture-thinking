package com.thinking.tennis.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRepository {

    Alert save(Alert alert);

    Optional<Alert> findById(UUID alertId);

    List<Alert> findByOwner(UserId owner);

    Optional<Alert> findWatchingByCondition(UserId owner, String courtId, LocalDate date, TimeWindow slot);

    void markChecked(String courtId, LocalDate date, Instant checkedAt);
}
