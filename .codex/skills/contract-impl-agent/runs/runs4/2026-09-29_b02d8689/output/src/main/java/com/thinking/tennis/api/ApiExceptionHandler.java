package com.thinking.tennis.api;

import com.thinking.tennis.app.UseCaseException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final int DEFAULT_INTERNAL_RETRY_AFTER_SECONDS = 5;
    private static final int STORAGE_RETRY_AFTER_SECONDS = 1;
    private static final int UPSTREAM_RETRY_AFTER_SECONDS = 20;

    @ExceptionHandler(UseCaseException.class)
    public ResponseEntity<ProblemResponse> handleUseCase(
            UseCaseException exception,
            HttpServletRequest request) {
        HttpStatus status = statusFor(exception.code(), request);
        Integer retryAfterSeconds = retryAfterSeconds(exception, status);
        ProblemResponse body = body(exception.code(), exception.getMessage(), status, request, retryAfterSeconds);
        return response(status, retryAfterSeconds, body);
    }

    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
    public ResponseEntity<ProblemResponse> handleBadRequest(
            RuntimeException exception,
            HttpServletRequest request) {
        UseCaseException.Code code = UseCaseException.Code.VALIDATION_FAILED;
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return response(status, null, body(code, "요청 값이 올바르지 않습니다", status, request, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        UseCaseException.Code code = UseCaseException.Code.INTERNAL_ERROR;
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        Integer retryAfterSeconds = DEFAULT_INTERNAL_RETRY_AFTER_SECONDS;
        return response(status, retryAfterSeconds, body(code, "요청을 처리하지 못했습니다", status, request, retryAfterSeconds));
    }

    private ResponseEntity<ProblemResponse> response(
            HttpStatus status,
            Integer retryAfterSeconds,
            ProblemResponse body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        if (retryAfterSeconds != null) {
            headers.set(HttpHeaders.RETRY_AFTER, retryAfterSeconds.toString());
        }
        return new ResponseEntity<>(body, headers, status);
    }

    private ProblemResponse body(
            UseCaseException.Code code,
            String detail,
            HttpStatus status,
            HttpServletRequest request,
            Integer retryAfterSeconds) {
        boolean retryable = status.is5xxServerError();
        return new ProblemResponse(
                "https://api.tennis-alert.example.com/problems/" + kebab(code.name()),
                title(code),
                status.value(),
                detail,
                request.getRequestURI(),
                code.name(),
                retryable,
                retryAfterSeconds,
                status.is5xxServerError() ? UUID.randomUUID().toString() : null);
    }

    private HttpStatus statusFor(UseCaseException.Code code, HttpServletRequest request) {
        return switch (code) {
            case VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case COURT_NOT_SUPPORTED -> request.getRequestURI().endsWith("/availability")
                    ? HttpStatus.NOT_FOUND : HttpStatus.UNPROCESSABLE_ENTITY;
            case SLOT_NOT_SUPPORTED, ALERT_WINDOW_CLOSED -> HttpStatus.UNPROCESSABLE_ENTITY;
            case ALERT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case IDEMPOTENCY_KEY_REUSED -> HttpStatus.CONFLICT;
            case UPSTREAM_RESPONSE_UNREADABLE -> HttpStatus.BAD_GATEWAY;
            case UPSTREAM_UNAVAILABLE, STORAGE_TIMEOUT, CONCURRENT_UPDATE_CONFLICT -> HttpStatus.SERVICE_UNAVAILABLE;
            case UPSTREAM_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private Integer retryAfterSeconds(UseCaseException exception, HttpStatus status) {
        if (!status.is5xxServerError()) {
            return null;
        }
        if (exception.retryAfterSeconds() != null) {
            return exception.retryAfterSeconds();
        }
        return switch (exception.code()) {
            case STORAGE_TIMEOUT, CONCURRENT_UPDATE_CONFLICT -> STORAGE_RETRY_AFTER_SECONDS;
            case UPSTREAM_TIMEOUT, UPSTREAM_UNAVAILABLE, UPSTREAM_RESPONSE_UNREADABLE ->
                    UPSTREAM_RETRY_AFTER_SECONDS;
            default -> DEFAULT_INTERNAL_RETRY_AFTER_SECONDS;
        };
    }

    private String title(UseCaseException.Code code) {
        return switch (code) {
            case VALIDATION_FAILED -> "요청 값이 올바르지 않습니다";
            case UNAUTHENTICATED -> "인증이 필요합니다";
            case COURT_NOT_SUPPORTED -> "지원하지 않는 코트입니다";
            case SLOT_NOT_SUPPORTED -> "지원하지 않는 시간대입니다";
            case ALERT_WINDOW_CLOSED -> "이미 지난 시간대입니다";
            case ALERT_NOT_FOUND -> "신청을 찾을 수 없습니다";
            case IDEMPOTENCY_KEY_REUSED -> "이미 다른 요청에 쓰인 멱등 키입니다";
            case UPSTREAM_TIMEOUT, UPSTREAM_UNAVAILABLE, UPSTREAM_RESPONSE_UNREADABLE -> "예약 상태를 확인하지 못했습니다";
            case STORAGE_TIMEOUT, CONCURRENT_UPDATE_CONFLICT -> "일시적으로 처리할 수 없습니다";
            case INTERNAL_ERROR -> "요청을 처리하지 못했습니다";
        };
    }

    private String kebab(String value) {
        return value.toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
