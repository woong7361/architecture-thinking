package com.thinking.tennis.app;

public final class AppExceptions {

    private AppExceptions() {
    }

    public enum ErrorCode {
        VALIDATION_FAILED,
        UNAUTHENTICATED,
        COURT_NOT_SUPPORTED,
        SLOT_NOT_SUPPORTED,
        ALERT_WINDOW_CLOSED,
        ALERT_NOT_FOUND,
        IDEMPOTENCY_KEY_REUSED,
        UPSTREAM_TIMEOUT,
        UPSTREAM_UNAVAILABLE,
        UPSTREAM_RESPONSE_UNREADABLE,
        STORAGE_TIMEOUT,
        CONCURRENT_UPDATE_CONFLICT,
        INTERNAL_ERROR
    }

    public static class AppException extends RuntimeException {

        private final ErrorCode code;

        public AppException(ErrorCode code, String message) {
            super(message);
            this.code = code;
        }

        public ErrorCode code() {
            return code;
        }
    }

    public static final class CourtNotSupported extends AppException {
        public CourtNotSupported(String courtId) {
            super(ErrorCode.COURT_NOT_SUPPORTED, "courtId: " + courtId);
        }
    }

    public static final class SlotNotSupported extends AppException {
        public SlotNotSupported(String message) {
            super(ErrorCode.SLOT_NOT_SUPPORTED, message);
        }
    }

    public static final class AlertWindowClosed extends AppException {
        public AlertWindowClosed() {
            super(ErrorCode.ALERT_WINDOW_CLOSED, "이용 시작 시각이 지나 감시할 수 없습니다");
        }
    }

    public static final class AlertNotFound extends AppException {
        public AlertNotFound() {
            super(ErrorCode.ALERT_NOT_FOUND, "신청을 찾을 수 없습니다");
        }
    }

    public static final class IdempotencyKeyReused extends AppException {
        public IdempotencyKeyReused() {
            super(ErrorCode.IDEMPOTENCY_KEY_REUSED, "같은 Idempotency-Key로 내용이 다른 요청이 앞서 처리됐습니다. 새 키로 보내십시오.");
        }
    }

    public static final class ConcurrentUpdateConflict extends AppException {
        public ConcurrentUpdateConflict() {
            super(ErrorCode.CONCURRENT_UPDATE_CONFLICT, "같은 신청을 동시에 고치려는 요청이 있었습니다. 같은 요청을 그대로 다시 보내면 됩니다.");
        }
    }

    public static final class UpstreamUnavailable extends AppException {
        public UpstreamUnavailable(ErrorCode code, String message) {
            super(code, message);
        }
    }
}
