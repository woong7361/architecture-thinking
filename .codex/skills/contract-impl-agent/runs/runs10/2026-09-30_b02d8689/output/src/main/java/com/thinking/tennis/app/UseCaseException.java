package com.thinking.tennis.app;

public class UseCaseException extends RuntimeException {

    public UseCaseException(int status,
                            String code,
                            String detail,
                            boolean retryable,
                            Integer retryAfterSeconds) {
        this(code, detail, status, retryable, retryAfterSeconds);
    }

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

    private final String code;
    private final int status;
    private final boolean retryable;
    private final Integer retryAfterSeconds;
    private final String detail;

    public UseCaseException(String code, int status, boolean retryable) {
        this(code, status, retryable, null, null);
    }

    public UseCaseException(String code, int status, boolean retryable, Integer retryAfterSeconds) {
        this(code, status, retryable, retryAfterSeconds, null);
    }

    public UseCaseException(String code, int status, boolean retryable, Integer retryAfterSeconds, String detail) {
        super(detail);
        this.code = code;
        this.status = status;
        this.retryable = retryable;
        this.retryAfterSeconds = retryAfterSeconds;
        this.detail = detail;
    }

    public UseCaseException(String code, String detail, int status, boolean retryable) {
        this(code, status, retryable, null, detail);
    }

    public UseCaseException(String code, String detail, int status, boolean retryable, Integer retryAfterSeconds) {
        this(code, status, retryable, retryAfterSeconds, detail);
    }

    public UseCaseException(Object code, int status, boolean retryable) {
        this(stringCode(code), status, retryable, null, null);
    }

    public UseCaseException(Object code, int status, boolean retryable, Integer retryAfterSeconds) {
        this(stringCode(code), status, retryable, retryAfterSeconds, null);
    }

    public UseCaseException(Object code, int status, boolean retryable, Integer retryAfterSeconds, String detail) {
        this(stringCode(code), status, retryable, retryAfterSeconds, detail);
    }

    public UseCaseException(Object code, String detail, int status, boolean retryable) {
        this(stringCode(code), status, retryable, null, detail);
    }

    public UseCaseException(Object code, String detail, int status, boolean retryable, Integer retryAfterSeconds) {
        this(stringCode(code), status, retryable, retryAfterSeconds, detail);
    }

    public String code() {
        return code;
    }

    public String getCode() {
        return code;
    }

    public String errorCode() {
        return code;
    }

    public int status() {
        return status;
    }

    public int getStatus() {
        return status;
    }

    public int httpStatus() {
        return status;
    }

    public boolean retryable() {
        return retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Integer retryAfterSeconds() {
        return retryAfterSeconds;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public String detail() {
        return detail;
    }

    public String getDetail() {
        return detail;
    }

    public static UseCaseException courtNotSupported(String detail) {
        return new UseCaseException(Code.COURT_NOT_SUPPORTED, detail, 404, false);
    }

    public static UseCaseException slotNotSupported(String detail) {
        return new UseCaseException(Code.SLOT_NOT_SUPPORTED, detail, 422, false);
    }

    public static UseCaseException alertWindowClosed(String detail) {
        return new UseCaseException(Code.ALERT_WINDOW_CLOSED, detail, 422, false);
    }

    public static UseCaseException alertNotFound() {
        return new UseCaseException(Code.ALERT_NOT_FOUND, 404, false);
    }

    public static UseCaseException idempotencyKeyReused(String detail) {
        return new UseCaseException(Code.IDEMPOTENCY_KEY_REUSED, detail, 409, false);
    }

    public static UseCaseException upstreamTimeout(String detail, int retryAfterSeconds) {
        return new UseCaseException(Code.UPSTREAM_TIMEOUT, detail, 504, true, retryAfterSeconds);
    }

    public static UseCaseException upstreamUnavailable(String detail, int retryAfterSeconds) {
        return new UseCaseException(Code.UPSTREAM_UNAVAILABLE, detail, 503, true, retryAfterSeconds);
    }

    public static UseCaseException upstreamResponseUnreadable(String detail, int retryAfterSeconds) {
        return new UseCaseException(Code.UPSTREAM_RESPONSE_UNREADABLE, detail, 502, true, retryAfterSeconds);
    }

    public static UseCaseException storageTimeout(int retryAfterSeconds) {
        return new UseCaseException(Code.STORAGE_TIMEOUT, 503, true, retryAfterSeconds);
    }

    public static UseCaseException concurrentUpdateConflict(String detail, int retryAfterSeconds) {
        return new UseCaseException(Code.CONCURRENT_UPDATE_CONFLICT, detail, 503, true, retryAfterSeconds);
    }

    public static UseCaseException internalError() {
        return new UseCaseException(Code.INTERNAL_ERROR, 500, true, 5);
    }

    private static String stringCode(Object code) {
        return code instanceof Enum<?> enumCode ? enumCode.name() : String.valueOf(code);
    }
}
