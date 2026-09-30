package com.thinking.tennis.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CourtAvailabilitySnapshot(String courtId,
                                        String courtName,
                                        LocalDate date,
                                        Instant confirmedAt,
                                        boolean stale,
                                        String staleReason,
                                        String reservationUrl,
                                        List<AvailabilitySlot> slots) {
}
