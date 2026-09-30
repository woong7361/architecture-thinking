package com.thinking.tennis.app;

import com.thinking.tennis.domain.AlertSnapshot;

public record CreateAlertResult(AlertSnapshot alert, int status, boolean replayed) {
}
