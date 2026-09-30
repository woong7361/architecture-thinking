package com.thinking.tennis.api;

public final class RequestValidationException extends RuntimeException {

    public RequestValidationException(String message) {
        super(message);
    }
}
