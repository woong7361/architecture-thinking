package com.thinking.tennis.app;

public class AlertApplicationException extends RuntimeException {

    public AlertApplicationException(String message) {
        super(message);
    }

    public AlertApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
