package com.thinking.tennis.app;

import org.springframework.http.HttpStatus;

public class ApplicationException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode code;
    private final String title;
    private final String detail;
    private final boolean retryable;
    private final Integer retryAfterSeconds;

    public ApplicationException(HttpStatus status, ErrorCode code, String title, String detail,
                                boolean retryable, Integer retryAfterSeconds) {
        super(detail == null ? title : detail);
        this.status = status;
        this.code = code;
        this.title = title;
        this.detail = detail;
        this.retryable = retryable;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static ApplicationException validation(String detail) {
        return new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
                "요청 값이 올바르지 않습니다", detail, false, null);
    }

    public static ApplicationException unauthenticated() {
        return new ApplicationException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED,
                "인증이 필요합니다", null, false, null);
    }

    public static ApplicationException alertNotFound() {
        return new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ALERT_NOT_FOUND,
                "신청을 찾을 수 없습니다", null, false, null);
    }

    public static ApplicationException courtNotSupported(String detail, HttpStatus status) {
        return new ApplicationException(status, ErrorCode.COURT_NOT_SUPPORTED,
                "지원하지 않는 코트입니다", detail, false, null);
    }

    public static ApplicationException slotNotSupported(String detail) {
        return new ApplicationException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.SLOT_NOT_SUPPORTED,
                "지원하지 않는 시간대입니다", detail, false, null);
    }

    public static ApplicationException windowClosed() {
        return new ApplicationException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.ALERT_WINDOW_CLOSED,
                "이미 지난 시간대입니다", "이용 시작 시각이 지나 감시할 수 없습니다", false, null);
    }

    public static ApplicationException idempotencyKeyReused() {
        return new ApplicationException(HttpStatus.CONFLICT, ErrorCode.IDEMPOTENCY_KEY_REUSED,
                "이미 다른 요청에 쓰인 멱등 키입니다",
                "같은 Idempotency-Key로 내용이 다른 요청이 앞서 처리됐습니다. 새 키로 보내십시오.", false, null);
    }

    public static ApplicationException concurrentUpdate() {
        return new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.CONCURRENT_UPDATE_CONFLICT,
                "일시적으로 처리할 수 없습니다",
                "같은 신청을 동시에 고치려는 요청이 있었습니다. 같은 요청을 그대로 다시 보내면 됩니다.", true, 1);
    }

    public static ApplicationException internalError() {
        return new ApplicationException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "요청을 처리하지 못했습니다", null, true, 5);
    }

    public static ApplicationException upstream(ErrorCode code) {
        return switch (code) {
            case UPSTREAM_TIMEOUT -> new ApplicationException(HttpStatus.GATEWAY_TIMEOUT, code,
                    "예약 상태를 확인하지 못했습니다", "예약처가 제때 응답하지 않았습니다. 빈자리가 없다는 뜻은 아닙니다.", true, 20);
            case UPSTREAM_RESPONSE_UNREADABLE -> new ApplicationException(HttpStatus.BAD_GATEWAY, code,
                    "예약 상태를 확인하지 못했습니다", "예약처 응답을 읽지 못했습니다. 빈자리가 없다는 뜻은 아닙니다.", true, 20);
            case UPSTREAM_UNAVAILABLE -> new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, code,
                    "예약 상태를 확인하지 못했습니다", "예약처에 연결하지 못했습니다. 빈자리가 없다는 뜻은 아닙니다.", true, 20);
            default -> internalError();
        };
    }

    public HttpStatus status() {
        return status;
    }

    public ErrorCode code() {
        return code;
    }

    public String title() {
        return title;
    }

    public String detail() {
        return detail;
    }

    public boolean retryable() {
        return retryable;
    }

    public Integer retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
