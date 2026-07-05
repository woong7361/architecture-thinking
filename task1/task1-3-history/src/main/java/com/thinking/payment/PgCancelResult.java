package com.thinking.payment;

public enum PgCancelResult {
    SUCCEEDED,
    REJECTED,
    TIMED_OUT;

    public static PgCancelResult succeeded() {
        return SUCCEEDED;
    }

    public static PgCancelResult rejected() {
        return REJECTED;
    }

    public static PgCancelResult timedOut() {
        return TIMED_OUT;
    }

    boolean isSucceeded() {
        return this == SUCCEEDED;
    }

    boolean isRejected() {
        return this == REJECTED;
    }

    boolean isTimedOut() {
        return this == TIMED_OUT;
    }
}
