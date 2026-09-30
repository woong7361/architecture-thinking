package com.thinking.tennis.app;

import org.springframework.http.HttpStatus;

public final class ApplicationFailure extends RuntimeException {

    private final FailureCode code;
    private final HttpStatus status;
    private final boolean retryable;
    private final Integer retryAfterSeconds;

    public ApplicationFailure(FailureCode code,
                              HttpStatus status,
                              boolean retryable,
                              Integer retryAfterSeconds,
                              String detail) {
        super(detail);
        this.code = code;
        this.status = status;
        this.retryable = retryable;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public ApplicationFailure(FailureCode code, HttpStatus status, String detail) {
        this(code, status, false, null, detail);
    }

    public ApplicationFailure(FailureCode code, String detail) {
        this(code, HttpStatus.INTERNAL_SERVER_ERROR, false, null, detail);
    }

    public ApplicationFailure(FailureCode code,
                              HttpStatus status,
                              boolean retryable,
                              int retryAfterSeconds,
                              String detail) {
        this(code, status, retryable, Integer.valueOf(retryAfterSeconds), detail);
    }

    public ApplicationFailure(FailureCode code,
                              int status,
                              boolean retryable,
                              Integer retryAfterSeconds,
                              String detail) {
        this(code, HttpStatus.valueOf(status), retryable, retryAfterSeconds, detail);
    }

    public ApplicationFailure(FailureCode code,
                              HttpStatus status,
                              String detail,
                              Throwable cause) {
        this(code, status, false, null, detail);
        initCause(cause);
    }

    public ApplicationFailure(FailureCode code,
                              HttpStatus status,
                              boolean retryable,
                              Integer retryAfterSeconds,
                              String detail,
                              Throwable cause) {
        this(code, status, retryable, retryAfterSeconds, detail);
        initCause(cause);
    }

    public FailureCode code() {
        return code;
    }

    public FailureCode failureCode() {
        return code;
    }

    public HttpStatus status() {
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

    public Integer retryAfter() {
        return retryAfterSeconds;
    }

    public int statusCode() {
        return status.value();
    }

    public String detail() {
        return getMessage();
    }

    public HttpStatus httpStatus() {
        return status;
    }
}
