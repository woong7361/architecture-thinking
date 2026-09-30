package com.thinking.tennis.domain;

import com.thinking.tennis.port.CourtAvailabilityPort;

import java.time.Instant;

public final class AvailabilityCacheEntry {
    private final AvailabilitySnapshot snapshot;
    private final Instant lastAttemptAt;
    private final CourtAvailabilityPort.UpstreamFailureReason failureReason;
    private final Instant failureRetryAt;

    private AvailabilityCacheEntry(
            AvailabilitySnapshot snapshot,
            Instant lastAttemptAt,
            CourtAvailabilityPort.UpstreamFailureReason failureReason,
            Instant failureRetryAt
    ) {
        this.snapshot = snapshot;
        this.lastAttemptAt = lastAttemptAt;
        this.failureReason = failureReason;
        this.failureRetryAt = failureRetryAt;
    }

    public static AvailabilityCacheEntry success(AvailabilitySnapshot snapshot) {
        return new AvailabilityCacheEntry(snapshot, snapshot.confirmedAt(), null, null);
    }

    public static AvailabilityCacheEntry failure(
            AvailabilitySnapshot lastSuccess,
            Instant lastAttemptAt,
            CourtAvailabilityPort.UpstreamFailureReason failureReason,
            Instant failureRetryAt
    ) {
        return new AvailabilityCacheEntry(lastSuccess, lastAttemptAt, failureReason, failureRetryAt);
    }

    public AvailabilitySnapshot snapshot() {
        return snapshot;
    }

    public Instant lastAttemptAt() {
        return lastAttemptAt;
    }

    public CourtAvailabilityPort.UpstreamFailureReason failureReason() {
        return failureReason;
    }

    public Instant failureRetryAt() {
        return failureRetryAt;
    }

    public boolean hasFreshSnapshot(Instant now, long freshnessSeconds) {
        return snapshot != null
                && snapshot.confirmedAt().plusSeconds(freshnessSeconds).isAfter(now);
    }

    public boolean suppressesRetry(Instant now) {
        return failureRetryAt != null && failureRetryAt.isAfter(now);
    }
}
