package com.thinking.payment;

public enum PgCancelResult {
    SUCCEEDED,
    REJECTED;

    public static PgCancelResult succeeded() {
        return SUCCEEDED;
    }

    public static PgCancelResult rejected() {
        return REJECTED;
    }

    boolean isSucceeded() {
        return this == SUCCEEDED;
    }

    boolean isRejected() {
        return this == REJECTED;
    }
}
