package com.thinking.tennis.api;

import com.thinking.tennis.app.AppExceptions;
import com.thinking.tennis.app.AppExceptions.AppException;
import com.thinking.tennis.app.AppExceptions.ErrorCode;
import com.thinking.tennis.app.TennisAlertConstants;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UnauthenticatedException.class)
    ResponseEntity<Map<String, Object>> unauthenticated(HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED, "인증이 필요합니다", null, false, null, request);
    }

    @ExceptionHandler(ApiValidationException.class)
    ResponseEntity<Map<String, Object>> validation(ApiValidationException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "요청 값이 올바르지 않습니다", ex.getMessage(), false, null, request);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<Map<String, Object>> springValidation(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "요청 값이 올바르지 않습니다", ex.getMessage(), false, null, request);
    }

    @ExceptionHandler(AppException.class)
    ResponseEntity<Map<String, Object>> app(AppException ex, HttpServletRequest request) {
        return switch (ex.code()) {
            case COURT_NOT_SUPPORTED -> courtNotSupported(ex, request);
            case SLOT_NOT_SUPPORTED -> problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.code(), "지원하지 않는 시간대입니다", ex.getMessage(), false, null, request);
            case ALERT_WINDOW_CLOSED -> problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.code(), "이미 지난 시간대입니다", ex.getMessage(), false, null, request);
            case ALERT_NOT_FOUND -> problem(HttpStatus.NOT_FOUND, ex.code(), "신청을 찾을 수 없습니다", null, false, null, request);
            case IDEMPOTENCY_KEY_REUSED -> problem(HttpStatus.CONFLICT, ex.code(), "이미 다른 요청에 쓰인 멱등 키입니다", ex.getMessage(), false, null, request);
            case CONCURRENT_UPDATE_CONFLICT -> retryable(HttpStatus.SERVICE_UNAVAILABLE, ex.code(), "일시적으로 처리할 수 없습니다", ex.getMessage(), TennisAlertConstants.CONTENTION_RETRY_AFTER_SECONDS, request);
            case UPSTREAM_TIMEOUT -> retryable(HttpStatus.GATEWAY_TIMEOUT, ex.code(), "예약 상태를 확인하지 못했습니다", ex.getMessage(), (int) TennisAlertConstants.AVAILABILITY_CHECK_INTERVAL.toSeconds(), request);
            case UPSTREAM_RESPONSE_UNREADABLE -> retryable(HttpStatus.BAD_GATEWAY, ex.code(), "예약 상태를 확인하지 못했습니다", ex.getMessage(), (int) TennisAlertConstants.AVAILABILITY_CHECK_INTERVAL.toSeconds(), request);
            case UPSTREAM_UNAVAILABLE -> retryable(HttpStatus.SERVICE_UNAVAILABLE, ex.code(), "예약 상태를 확인하지 못했습니다", ex.getMessage(), (int) TennisAlertConstants.AVAILABILITY_CHECK_INTERVAL.toSeconds(), request);
            case STORAGE_TIMEOUT -> retryable(HttpStatus.SERVICE_UNAVAILABLE, ex.code(), "일시적으로 처리할 수 없습니다", null, TennisAlertConstants.CONTENTION_RETRY_AFTER_SECONDS, request);
            case INTERNAL_ERROR, VALIDATION_FAILED, UNAUTHENTICATED -> internal(request);
        };
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> unhandled(Exception ex, HttpServletRequest request) {
        return internal(request);
    }

    private ResponseEntity<Map<String, Object>> courtNotSupported(AppException ex, HttpServletRequest request) {
        HttpStatus status = "GET".equals(request.getMethod()) && request.getRequestURI().contains("/courts/")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.UNPROCESSABLE_ENTITY;
        return problem(status, ex.code(), "지원하지 않는 코트입니다", ex.getMessage(), false, null, request);
    }

    private ResponseEntity<Map<String, Object>> internal(HttpServletRequest request) {
        return retryable(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "요청을 처리하지 못했습니다", null, TennisAlertConstants.INTERNAL_ERROR_RETRY_AFTER_SECONDS, request);
    }

    private ResponseEntity<Map<String, Object>> retryable(HttpStatus status,
                                                          ErrorCode code,
                                                          String title,
                                                          String detail,
                                                          int retryAfterSeconds,
                                                          HttpServletRequest request) {
        return problem(status, code, title, detail, true, retryAfterSeconds, request);
    }

    private ResponseEntity<Map<String, Object>> problem(HttpStatus status,
                                                        ErrorCode code,
                                                        String title,
                                                        String detail,
                                                        boolean retryable,
                                                        Integer retryAfterSeconds,
                                                        HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://api.tennis-alert.example.com/problems/" + slug(code));
        body.put("title", title);
        body.put("status", status.value());
        if (detail != null) {
            body.put("detail", detail);
        }
        body.put("instance", request.getRequestURI());
        body.put("code", code.name());
        body.put("retryable", retryable);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        if (retryableAfterIsPresent(retryable, retryAfterSeconds)) {
            body.put("retryAfterSeconds", retryAfterSeconds);
            headers.set(HttpHeaders.RETRY_AFTER, retryAfterSeconds.toString());
        }
        if (status.is5xxServerError()) {
            body.put("traceId", java.util.UUID.randomUUID().toString());
        }
        return new ResponseEntity<>(body, headers, status);
    }

    private boolean retryableAfterIsPresent(boolean retryable, Integer retryAfterSeconds) {
        return retryable && retryAfterSeconds != null;
    }

    private String slug(ErrorCode code) {
        return code.name().toLowerCase().replace('_', '-');
    }
}
