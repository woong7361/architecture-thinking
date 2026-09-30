package com.thinking.tennis.app;

import com.thinking.tennis.domain.DomainModels.Alert;

public record CreateAlertResult(int status,
                                String location,
                                Alert alert,
                                boolean replayed) {
}
