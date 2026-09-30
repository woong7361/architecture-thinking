package com.thinking.tennis.app;

import java.time.Duration;
import java.time.ZoneId;
import java.util.regex.Pattern;

public final class TennisAlertConstants {

    public static final ZoneId COURT_ZONE = ZoneId.of("Asia/Seoul");
    public static final Duration AVAILABILITY_CHECK_INTERVAL = Duration.ofSeconds(20);
    public static final Duration CHECK_DELAY_THRESHOLD = AVAILABILITY_CHECK_INTERVAL.multipliedBy(3);
    public static final Duration IDEMPOTENCY_RETENTION = Duration.ofHours(24);
    public static final int CONTENTION_RETRY_AFTER_SECONDS = 1;
    public static final int INTERNAL_ERROR_RETRY_AFTER_SECONDS = 5;
    public static final Pattern COURT_ID_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");

    private TennisAlertConstants() {
    }
}
