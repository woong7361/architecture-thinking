package com.thinking.tennis.app;

import com.thinking.tennis.domain.Alert;

public record CreateAlertResult(Alert alert, int status, boolean replayed) {
}
