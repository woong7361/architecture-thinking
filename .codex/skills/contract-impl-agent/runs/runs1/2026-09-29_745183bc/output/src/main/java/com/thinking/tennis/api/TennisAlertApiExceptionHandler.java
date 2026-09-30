package com.thinking.tennis.api;

import com.thinking.tennis.app.AlertApplicationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class TennisAlertApiExceptionHandler {

    @ExceptionHandler(AlertApplicationService.AppFailure.class)
    public ResponseEntity<Map<String, Object>> handleAppFailure(AlertApplicationService.AppFailure failure,
                                                                HttpServletRequest request) {
        ProblemSpec spec = specFor(failure.code(), request.getRequestURI());
        int retryAfter = failure.retryAfterSeconds().orElse(spec.retryAfterSeconds());
        return build(spec, request.getRequestURI(), retryAfter);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception failure, HttpServletRequest request) {
        return build(specFor(AlertApplicationService.FailureCode.VALIDATION_FAILED, request.getRequestURI()),
                request.getRequestURI(), 0);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception failure, HttpServletRequest request) {
        return build(specFor(AlertApplicationService.FailureCode.INTERNAL_ERROR, request.getRequestURI()),
                request.getRequestURI(), 5);
    }

    private ResponseEntity<Map<String, Object>> build(ProblemSpec spec, String instance, int retryAfterSeconds) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/problem+json"));
        if (spec.retryable()) {
            headers.set(HttpHeaders.RETRY_AFTER, Integer.toString(retryAfterSeconds));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", spec.type());
        body.put("title", spec.title());
        body.put("status", spec.status());
        if (spec.detail() != null) {
            body.put("detail", spec.detail());
        }
        body.put("instance", instance);
        body.put("code", spec.code());
        body.put("retryable", spec.retryable());
        if (spec.retryable()) {
            body.put("retryAfterSeconds", retryAfterSeconds);
        }
        if (spec.status() >= 500) {
            body.put("traceId", UUID.randomUUID().toString());
        }
        return new ResponseEntity<>(body, headers, HttpStatus.valueOf(spec.status()));
    }

    private ProblemSpec specFor(AlertApplicationService.FailureCode code, String path) {
        boolean createAlert = path.endsWith("/alerts");
        return switch (code) {
            case VALIDATION_FAILED -> new ProblemSpec("validation-failed", "요청 값이 올바르지 않습니다", 400,
                    "요청이 계약의 형식을 만족하지 않습니다", code, false, 0);
            case UNAUTHENTICATED -> new ProblemSpec("unauthenticated", "인증이 필요합니다", 401, null, code, false, 0);
            case COURT_NOT_SUPPORTED -> new ProblemSpec("court-not-supported", "지원하지 않는 코트입니다",
                    createAlert ? 422 : 404, null, code, false, 0);
            case SLOT_NOT_SUPPORTED -> new ProblemSpec("slot-not-supported", "지원하지 않는 시간대입니다", 422,
                    "코트가 그 날짜에 운영하지 않는 시간대입니다", code, false, 0);
            case ALERT_WINDOW_CLOSED -> new ProblemSpec("alert-window-closed", "이미 지난 시간대입니다", 422,
                    "이용 시작 시각이 지나 감시할 수 없습니다", code, false, 0);
            case ALERT_NOT_FOUND -> new ProblemSpec("alert-not-found", "신청을 찾을 수 없습니다", 404, null, code, false, 0);
            case IDEMPOTENCY_KEY_REUSED -> new ProblemSpec("idempotency-key-reused", "이미 다른 요청에 쓰인 멱등 키입니다",
                    409, "같은 Idempotency-Key로 내용이 다른 요청이 앞서 처리됐습니다. 새 키로 보내십시오.", code, false, 0);
            case UPSTREAM_TIMEOUT -> new ProblemSpec("upstream-timeout", "예약 상태를 확인하지 못했습니다", 504,
                    "예약처가 제때 응답하지 않았습니다. 빈자리가 없다는 뜻은 아닙니다.", code, true, 20);
            case UPSTREAM_UNAVAILABLE -> new ProblemSpec("upstream-unavailable", "예약 상태를 확인하지 못했습니다", 503,
                    "예약처에 연결하지 못했습니다. 빈자리가 없다는 뜻은 아닙니다.", code, true, 20);
            case UPSTREAM_RESPONSE_UNREADABLE -> new ProblemSpec("upstream-response-unreadable", "예약 상태를 확인하지 못했습니다",
                    502, "예약처 응답을 읽지 못했습니다. 빈자리가 없다는 뜻은 아닙니다.", code, true, 20);
            case STORAGE_TIMEOUT -> new ProblemSpec("storage-timeout", "일시적으로 처리할 수 없습니다", 503,
                    null, code, true, 1);
            case CONCURRENT_UPDATE_CONFLICT -> new ProblemSpec("concurrent-update-conflict", "일시적으로 처리할 수 없습니다",
                    503, "같은 요청을 그대로 다시 보내면 됩니다.", code, true, 1);
            case INTERNAL_ERROR -> new ProblemSpec("internal-error", "요청을 처리하지 못했습니다", 500, null, code, true, 5);
        };
    }

    private record ProblemSpec(String typeSlug,
                               String title,
                               int status,
                               String detail,
                               AlertApplicationService.FailureCode code,
                               boolean retryable,
                               int retryAfterSeconds) {
        private URI type() {
            return URI.create("https://api.tennis-alert.example.com/problems/" + typeSlug.toLowerCase(Locale.ROOT));
        }
    }
}
