package com.thinking.tennis.app;

public class UseCaseException extends RuntimeException {
    private final FailureCode code;
    private final FailureContext context;
    private final boolean retryable;
    private final Integer retryAfterSeconds;

    public UseCaseException(
            FailureCode code,
            FailureContext context,
            String detail,
            boolean retryable,
            Integer retryAfterSeconds
    ) {
        super(detail);
        this.code = code;
        this.context = context;
        this.retryable = retryable;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public FailureCode code() {
        return code;
    }

    public FailureContext context() {
        return context;
    }

    public boolean retryable() {
        return retryable;
    }

    public Integer retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
