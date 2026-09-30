package com.thinking.tennis.api;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts application failures into the problem response used by the HTTP contract.
 *
 * <p>The application layer deliberately does not expose HTTP types. Exception names and their
 * small value accessors are used here so that this adapter remains the only place that assigns
 * transport status codes and headers.
 */
@RestControllerAdvice
public class TennisAlertExceptionHandler {

    private static final String PROBLEM_BASE =
            "https://api.tennis-alert.example.com/problems/";
    private static final int REFRESH_RETRY_AFTER_SECONDS = 20;
    private static final int SHORT_RETRY_AFTER_SECONDS = 1;
    private static final int INTERNAL_RETRY_AFTER_SECONDS = 5;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handle(Exception exception,
                                                       HttpServletRequest request) {
        Throwable failure = rootCause(exception);
        ErrorMapping mapping = map(failure, request.getRequestURI());
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", PROBLEM_BASE + kebab(mapping.code()));
        problem.put("title", mapping.title());
        problem.put("status", mapping.status().value());
        if (failure.getMessage() != null && !failure.getMessage().isBlank()) {
            problem.put("detail", failure.getMessage());
        }
        problem.put("instance", request.getRequestURI());
        problem.put("code", mapping.code());
        problem.put("retryable", mapping.retryable());
        if (mapping.retryable()) {
            problem.put("retryAfterSeconds", mapping.retryAfterSeconds());
        }
        if (mapping.status().is5xxServerError()) {
            problem.put("traceId", UUID.randomUUID().toString());
        }

        HttpHeaders headers = new HttpHeaders();
        if (mapping.retryable()) {
            headers.setRetryAfter(Duration.ofSeconds(mapping.retryAfterSeconds()));
        }
        return ResponseEntity.status(mapping.status()).headers(headers).body(problem);
    }

    private ErrorMapping map(Throwable failure, String requestUri) {
        String name = failure.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        if (containsAny(name, "validation", "missingrequestheader", "messagenotreadable",
                "typemismatch", "methodargumentnotvalid")) {
            return ErrorMapping.of(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", false, 0,
                    "요청 값이 올바르지 않습니다");
        }
        if (containsAny(name, "unauthenticated", "authentication")) {
            return ErrorMapping.of(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", false, 0,
                    "인증이 필요합니다");
        }
        if (containsAny(name, "notfound", "not_found")) {
            return ErrorMapping.of(HttpStatus.NOT_FOUND, "ALERT_NOT_FOUND", false, 0,
                    "신청을 찾을 수 없습니다");
        }
        if (containsAny(name, "idempotencykeyreused", "idempotency_key_reused")) {
            return ErrorMapping.of(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", false, 0,
                    "이미 다른 요청에 쓰인 멱등 키입니다");
        }
        if (containsAny(name, "slotnotsupported", "slot_not_supported")) {
            return ErrorMapping.of(HttpStatus.UNPROCESSABLE_ENTITY, "SLOT_NOT_SUPPORTED",
                    false, 0, "지원하지 않는 시간대입니다");
        }
        if (containsAny(name, "windowclosed", "window_closed")) {
            return ErrorMapping.of(HttpStatus.UNPROCESSABLE_ENTITY, "ALERT_WINDOW_CLOSED",
                    false, 0, "이미 지난 시간대입니다");
        }
        if (containsAny(name, "courtnotsupported", "court_not_supported")) {
            HttpStatus status = requestUri.contains("/courts/") ? HttpStatus.NOT_FOUND
                    : HttpStatus.UNPROCESSABLE_ENTITY;
            return ErrorMapping.of(status, "COURT_NOT_SUPPORTED", false, 0,
                    "지원하지 않는 코트입니다");
        }
        if (containsAny(name, "concurrent", "contention")) {
            return ErrorMapping.of(HttpStatus.SERVICE_UNAVAILABLE,
                    "CONCURRENT_UPDATE_CONFLICT", true, SHORT_RETRY_AFTER_SECONDS,
                    "일시적으로 처리할 수 없습니다");
        }
        if (containsAny(name, "storage", "timeout") && !containsAny(name, "upstream")) {
            return ErrorMapping.of(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_TIMEOUT", true,
                    SHORT_RETRY_AFTER_SECONDS, "일시적으로 처리할 수 없습니다");
        }
        if (containsAny(name, "checkfailed", "upstream")) {
            return mapUpstream(failure);
        }
        return ErrorMapping.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", true,
                INTERNAL_RETRY_AFTER_SECONDS, "요청을 처리하지 못했습니다");
    }

    private ErrorMapping mapUpstream(Throwable failure) {
        String reason = String.valueOf(invoke(failure, "reason")).toUpperCase(Locale.ROOT);
        if (reason.contains("TIMEOUT")) {
            return ErrorMapping.of(HttpStatus.GATEWAY_TIMEOUT, "UPSTREAM_TIMEOUT", true,
                    REFRESH_RETRY_AFTER_SECONDS, "예약 상태를 확인하지 못했습니다");
        }
        if (reason.contains("UNREADABLE")) {
            return ErrorMapping.of(HttpStatus.BAD_GATEWAY,
                    "UPSTREAM_RESPONSE_UNREADABLE", true, REFRESH_RETRY_AFTER_SECONDS,
                    "예약 상태를 확인하지 못했습니다");
        }
        return ErrorMapping.of(HttpStatus.SERVICE_UNAVAILABLE, "UPSTREAM_UNAVAILABLE", true,
                REFRESH_RETRY_AFTER_SECONDS, "예약 상태를 확인하지 못했습니다");
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static Object invoke(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return "";
        }
    }

    private static boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String kebab(String code) {
        return code.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private record ErrorMapping(HttpStatus status,
                                String code,
                                boolean retryable,
                                int retryAfterSeconds,
                                String title) {

        private static ErrorMapping of(HttpStatus status,
                                       String code,
                                       boolean retryable,
                                       int retryAfterSeconds,
                                       String title) {
            return new ErrorMapping(status, code, retryable, retryAfterSeconds, title);
        }
    }
}
