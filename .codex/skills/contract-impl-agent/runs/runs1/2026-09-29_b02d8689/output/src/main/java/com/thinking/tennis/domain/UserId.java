package com.thinking.tennis.domain;

public record UserId(String value) {

    public UserId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("user id is required");
        }
    }
}
