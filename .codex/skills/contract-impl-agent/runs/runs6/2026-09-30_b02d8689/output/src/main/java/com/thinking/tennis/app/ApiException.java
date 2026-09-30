package com.thinking.tennis.app;

/**
 * Application-level failure used by the HTTP adapter.
 *
 * <p>The adapter owns the HTTP representation; this exception only carries
 * the values needed to build that representation.
 */
public class ApiException extends RuntimeException {

    private final String code;
    private final int status;
    private final boolean retryable;
    private final Integer retryAfterSeconds;
    private final String detail;
    private final String traceId;

    public ApiException(Object... values) {
        this(
                firstString(values, "INTERNAL_ERROR"),
                firstInt(values, 500),
                firstBoolean(values, true),
                firstInteger(values, 5),
                firstStringAfter(values, "INTERNAL_ERROR"),
                null
        );
    }

    public ApiException(String code,
                        int status,
                        boolean retryable,
                        Integer retryAfterSeconds,
                        String detail,
                        String traceId) {
        super(detail);
        this.code = code;
        this.status = status;
        this.retryable = retryable;
        this.retryAfterSeconds = retryAfterSeconds;
        this.detail = detail;
        this.traceId = traceId;
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }

    public boolean retryable() {
        return retryable;
    }

    public Integer retryAfterSeconds() {
        return retryAfterSeconds;
    }

    public String detail() {
        return detail;
    }

    public String traceId() {
        return traceId;
    }

    private static String firstString(Object[] values, String fallback) {
        for (Object value : values) {
            if (value instanceof String string && !string.isBlank()) {
                return string;
            }
            if (value instanceof Enum<?> enumValue) {
                return enumValue.name();
            }
        }
        return fallback;
    }

    private static String firstStringAfter(Object[] values, String fallback) {
        String previous = null;
        for (Object value : values) {
            if (value instanceof String string) {
                if (previous != null) {
                    return string;
                }
                previous = string;
            }
        }
        return fallback;
    }

    private static int firstInt(Object[] values, int fallback) {
        for (Object value : values) {
            if (value instanceof Integer integer) {
                return integer;
            }
        }
        return fallback;
    }

    private static Integer firstInteger(Object[] values, Integer fallback) {
        for (Object value : values) {
            if (value instanceof Integer integer && integer > 0) {
                return integer;
            }
        }
        return fallback;
    }

    private static boolean firstBoolean(Object[] values, boolean fallback) {
        for (Object value : values) {
            if (value instanceof Boolean booleanValue) {
                return booleanValue;
            }
        }
        return fallback;
    }
}
