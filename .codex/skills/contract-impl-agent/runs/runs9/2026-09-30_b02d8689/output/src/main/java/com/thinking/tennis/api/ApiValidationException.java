package com.thinking.tennis.api;

class ApiValidationException extends RuntimeException {

    ApiValidationException(String message) {
        super(message);
    }
}
