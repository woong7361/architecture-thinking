package com.thinking.tennis.app;

public class ApiFailure extends RuntimeException {
    private final int status;
    private final String code;
    private final boolean retryable;
    private final Integer retryAfterSeconds;
    private final String detail;

    public ApiFailure(int status, String code, boolean retryable, Integer retryAfterSeconds, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
        this.retryable = retryable;
        this.retryAfterSeconds = retryAfterSeconds;
        this.detail = detail;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public boolean retryable() {
        return retryable;
    }

    public Integer retryAfterSeconds() {
        return retryAfterSeconds;
    }

    public String detail() {
        return detail;
    }

    public static ApiFailure unauthenticated() {
        return new ApiFailure(401, "UNAUTHENTICATED", false, null, "Authentication is required.");
    }

    public static ApiFailure notFound() {
        return new ApiFailure(404, "ALERT_NOT_FOUND", false, null, "Alert was not found.");
    }

    public static ApiFailure courtNotSupported(String courtId) {
        return new ApiFailure(422, "COURT_NOT_SUPPORTED", false, null, "Unsupported court: " + courtId);
    }

    public static ApiFailure courtNotSupportedPath(String courtId) {
        return new ApiFailure(404, "COURT_NOT_SUPPORTED", false, null, "Unsupported court: " + courtId);
    }

    public static ApiFailure slotNotSupported() {
        return new ApiFailure(422, "SLOT_NOT_SUPPORTED", false, null, "The requested slot is not supported.");
    }

    public static ApiFailure windowClosed() {
        return new ApiFailure(422, "ALERT_WINDOW_CLOSED", false, null, "The requested time window has already closed.");
    }

    public static ApiFailure idempotencyReused() {
        return new ApiFailure(409, "IDEMPOTENCY_KEY_REUSED", false, null, "The idempotency key was used for different content.");
    }

    public static ApiFailure contention() {
        return new ApiFailure(503, "CONCURRENT_UPDATE_CONFLICT", true, 1, "A concurrent update is still in progress.");
    }

    public static ApiFailure internal() {
        return new ApiFailure(500, "INTERNAL_ERROR", true, 5, "The request could not be processed.");
    }
}
