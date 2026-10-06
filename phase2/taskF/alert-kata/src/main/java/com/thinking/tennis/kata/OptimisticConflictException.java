package com.thinking.tennis.kata;

public class OptimisticConflictException extends RuntimeException {
    public OptimisticConflictException(long userId) {
        super("User version changed while subscribing: " + userId);
    }
}
