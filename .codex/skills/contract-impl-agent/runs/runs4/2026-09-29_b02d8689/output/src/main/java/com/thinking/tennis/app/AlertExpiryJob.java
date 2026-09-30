package com.thinking.tennis.app;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AlertExpiryJob {

    private static final long EXPIRY_SWEEP_FIXED_DELAY_MS = 1000L;

    private final AlertApplicationService alertApplicationService;

    public AlertExpiryJob(AlertApplicationService alertApplicationService) {
        this.alertApplicationService = alertApplicationService;
    }

    @Scheduled(fixedDelay = EXPIRY_SWEEP_FIXED_DELAY_MS)
    public void expireDueAlerts() {
        alertApplicationService.expireDueAlerts();
    }
}
