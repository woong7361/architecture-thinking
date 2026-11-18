package com.thinking.tennis.kata;

import java.time.Duration;

public record LockToken(String key, String token, Duration lease) {
}
