package com.thinking.tennis.kata;

public record SubscribeOutcome(
        Status status,
        Long subscriptionId,
        boolean ownedAtExit,
        ReleaseResult releaseResult,
        double waitMillis,
        double workMillis) {

    public enum Status {
        COMPLETED,
        NOT_ACQUIRED,
        LEASE_TOO_SHORT
    }
}
