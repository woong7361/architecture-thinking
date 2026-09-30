package com.thinking.tennis.api;

class RequestValidationException extends RuntimeException {

    RequestValidationException(String message) {
        super(message);
    }
}
