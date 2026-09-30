package com.thinking.tennis.app;

public final class ValidationFailure extends ApiFailure {
    public ValidationFailure(String detail) {
        super(400, "VALIDATION_FAILED", false, null, detail);
    }
}
