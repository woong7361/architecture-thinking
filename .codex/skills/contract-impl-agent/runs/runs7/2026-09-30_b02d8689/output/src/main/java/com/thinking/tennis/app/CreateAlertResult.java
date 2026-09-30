package com.thinking.tennis.app;

public record CreateAlertResult(
        AlertService.AlertView alert,
        int status,
        boolean replayed
) {
}
