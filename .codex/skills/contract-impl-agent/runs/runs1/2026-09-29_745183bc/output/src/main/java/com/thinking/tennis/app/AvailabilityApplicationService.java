package com.thinking.tennis.app;

import com.thinking.tennis.port.CourtAvailabilityPort;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.thinking.tennis.app.AlertApplicationService.AppFailure;
import static com.thinking.tennis.app.AlertApplicationService.CHECK_INTERVAL_SECONDS;
import static com.thinking.tennis.app.AlertApplicationService.FailureCode;

public final class AvailabilityApplicationService {

    private final CourtAvailabilityPort courtAvailabilityPort;
    private final Clock clock;
    private final ConcurrentMap<AvailabilityKey, SuccessfulAvailability> lastSuccesses = new ConcurrentHashMap<>();
    private final ConcurrentMap<AvailabilityKey, CompletableFuture<CheckOutcome>> inFlightChecks = new ConcurrentHashMap<>();

    public AvailabilityApplicationService(CourtAvailabilityPort courtAvailabilityPort, Clock clock) {
        this.courtAvailabilityPort = courtAvailabilityPort;
        this.clock = clock;
    }

    public CourtAvailabilityView getAvailability(String courtId, String dateText) {
        Objects.requireNonNull(dateText, "dateText");
        try {
            return getAvailability(courtId, LocalDate.parse(dateText, DateTimeFormatter.ISO_LOCAL_DATE));
        } catch (DateTimeParseException failure) {
            throw AppFailure.of(FailureCode.VALIDATION_FAILED);
        }
    }

    public CourtAvailabilityView getAvailability(String courtId, LocalDate date) {
        if (courtId == null || courtId.isBlank() || !courtId.matches("^[a-z0-9][a-z0-9-]*$")) {
            throw AppFailure.of(FailureCode.VALIDATION_FAILED);
        }
        if (courtAvailabilityPort.findCourt(courtId).isEmpty()) {
            throw AppFailure.of(FailureCode.COURT_NOT_SUPPORTED);
        }
        AvailabilityKey key = new AvailabilityKey(courtId, date);
        Instant now = clock.instant();
        SuccessfulAvailability cached = lastSuccesses.get(key);
        if (cached != null && !cached.confirmedAt().plusSeconds(CHECK_INTERVAL_SECONDS).isBefore(now)) {
            return cached.toView(false, null);
        }

        CheckOutcome outcome = runCoalescedCheck(key);
        if (outcome.success() != null) {
            lastSuccesses.put(key, outcome.success());
            return outcome.success().toView(false, null);
        }
        SuccessfulAvailability fallback = lastSuccesses.get(key);
        if (fallback != null) {
            return fallback.toView(true, outcome.failureReason().name());
        }
        throw failureFor(outcome.failureReason());
    }

    public Instant lastSuccessfulAt(String courtId, LocalDate date) {
        SuccessfulAvailability successfulAvailability = lastSuccesses.get(new AvailabilityKey(courtId, date));
        return successfulAvailability == null ? null : successfulAvailability.confirmedAt();
    }

    private CheckOutcome runCoalescedCheck(AvailabilityKey key) {
        CompletableFuture<CheckOutcome> future = inFlightChecks.computeIfAbsent(key, ignored ->
                CompletableFuture.supplyAsync(() -> checkOnce(key))
                        .whenComplete((result, failure) -> inFlightChecks.remove(key)));
        return future.join();
    }

    private CheckOutcome checkOnce(AvailabilityKey key) {
        try {
            CourtAvailabilityPort.CourtAvailabilityCheck check =
                    courtAvailabilityPort.checkAvailability(key.courtId(), key.date());
            return CheckOutcome.success(new SuccessfulAvailability(check.court().courtId(),
                    check.court().courtName(),
                    check.court().reservationUrl(),
                    check.date(),
                    check.confirmedAt(),
                    check.slots().stream()
                            .map(slot -> new AvailabilitySlotView(AlertApplicationService.formatTime(slot.startTime()),
                                    AlertApplicationService.formatTime(slot.endTime()), slot.available()))
                            .toList()));
        } catch (CourtAvailabilityPort.CourtNotSupportedException failure) {
            throw AppFailure.of(FailureCode.COURT_NOT_SUPPORTED);
        } catch (CourtAvailabilityPort.CheckFailed failure) {
            return CheckOutcome.failure(failure.reason());
        }
    }

    private AppFailure failureFor(CourtAvailabilityPort.UpstreamFailureReason reason) {
        return switch (reason) {
            case UPSTREAM_TIMEOUT -> AppFailure.retryable(FailureCode.UPSTREAM_TIMEOUT, CHECK_INTERVAL_SECONDS);
            case UPSTREAM_UNAVAILABLE -> AppFailure.retryable(FailureCode.UPSTREAM_UNAVAILABLE, CHECK_INTERVAL_SECONDS);
            case UPSTREAM_RESPONSE_UNREADABLE ->
                    AppFailure.retryable(FailureCode.UPSTREAM_RESPONSE_UNREADABLE, CHECK_INTERVAL_SECONDS);
        };
    }

    public record CourtAvailabilityView(String courtId,
                                        String courtName,
                                        String date,
                                        String confirmedAt,
                                        boolean stale,
                                        String staleReason,
                                        String reservationUrl,
                                        List<AvailabilitySlotView> slots) {
    }

    public record AvailabilitySlotView(String startTime, String endTime, boolean available) {
    }

    private record AvailabilityKey(String courtId, LocalDate date) {
    }

    private record SuccessfulAvailability(String courtId,
                                          String courtName,
                                          String reservationUrl,
                                          LocalDate date,
                                          Instant confirmedAt,
                                          List<AvailabilitySlotView> slots) {
        private CourtAvailabilityView toView(boolean stale, String staleReason) {
            return new CourtAvailabilityView(courtId, courtName, date.toString(), confirmedAt.toString(), stale, staleReason,
                    reservationUrl, slots);
        }
    }

    private record CheckOutcome(SuccessfulAvailability success,
                                CourtAvailabilityPort.UpstreamFailureReason failureReason) {
        private static CheckOutcome success(SuccessfulAvailability availability) {
            return new CheckOutcome(availability, null);
        }

        private static CheckOutcome failure(CourtAvailabilityPort.UpstreamFailureReason reason) {
            return new CheckOutcome(null, reason);
        }
    }
}
