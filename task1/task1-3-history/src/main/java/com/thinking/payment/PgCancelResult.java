package com.thinking.payment;

public enum PgCancelResult {
    SUCCEEDED;

    public static PgCancelResult succeeded() {
        return SUCCEEDED;
    }

    boolean isSucceeded() {
        return this == SUCCEEDED;
    }
}
