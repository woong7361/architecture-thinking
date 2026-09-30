package com.thinking.tennis.app;

public class UseCaseException extends RuntimeException {

    public enum Code {
        VALIDATION_FAILED,
        UNAUTHENTICATED,
        COURT_NOT_SUPPORTED,
        SLOT_NOT_SUPPORTED,
        ALERT_WINDOW_CLOSED,
        ALERT_NOT_FOUND,
        IDEMPOTENCY_KEY_REUSED,
        UPSTREAM_TIMEOUT,
        UPSTREAM_UNAVAILABLE,
        UPSTREAM_RESPONSE_UNREADABLE,
        STORAGE_TIMEOUT,
        CONCURRENT_UPDATE_CONFLICT,
        INTERNAL_ERROR
    }

    private final Code code;
    private final Integer retryAfterSeconds;

    public UseCaseException(Code code, String message) {
        this(code, message, null);
    }

    public UseCaseException(Code code, String message, Integer retryAfterSeconds) {
        super(message);
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Code code() {
        return code;
    }

    public Integer retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
