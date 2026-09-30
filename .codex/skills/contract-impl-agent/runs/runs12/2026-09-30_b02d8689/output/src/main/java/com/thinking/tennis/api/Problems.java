package com.thinking.tennis.api;

import com.thinking.tennis.api.dto.ErrorCodeValue;
import com.thinking.tennis.api.dto.ProblemResponse;
import com.thinking.tennis.app.WatchPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 실패를 계약이 정한 실패 응답으로 옮긴다.
 *
 * <p>에러 코드마다 실패 이름과 재시도 가능 여부와 기다릴 초가 정해져 있다. 같은 코드가 상태 코드만
 * 달라지는 자리가 있어(지원하지 않는 코트) 상태 코드는 부르는 쪽이 넘긴다.
 *
 * <p>{@code retryable} 이 true이면 {@code retryAfterSeconds} 와 {@code Retry-After} 헤더를 반드시 함께
 * 보낸다. 두 값은 같은 값이다. {@code traceId} 는 5xx에만 싣는다.
 */
final class Problems {

    /** 실패 종류를 설명하는 문서의 주소를 만드는 기준이다. */
    private static final String TYPE_BASE = "https://api.tennis-alert.example.com/problems/";

    /** 확인 실패로 나가는 응답이 기다릴 초다. 계약이 확인 간격을 따르라고 했다. */
    private static final int UPSTREAM_RETRY_AFTER_SECONDS = (int) WatchPolicy.CHECK_INTERVAL.getSeconds();

    /** 저장소와 경합으로 물러난 응답이 기다릴 초다. */
    private static final int CONTENTION_RETRY_AFTER_SECONDS = 1;

    /** 서버 내부 실패가 기다릴 초다. */
    private static final int INTERNAL_RETRY_AFTER_SECONDS = 5;

    private static final Map<ErrorCodeValue, String> TITLES = new EnumMap<>(ErrorCodeValue.class);
    private static final Map<ErrorCodeValue, Integer> RETRY_AFTER = new EnumMap<>(ErrorCodeValue.class);

    static {
        TITLES.put(ErrorCodeValue.VALIDATION_FAILED, "요청 값이 올바르지 않습니다");
        TITLES.put(ErrorCodeValue.UNAUTHENTICATED, "인증이 필요합니다");
        TITLES.put(ErrorCodeValue.COURT_NOT_SUPPORTED, "지원하지 않는 코트입니다");
        TITLES.put(ErrorCodeValue.SLOT_NOT_SUPPORTED, "지원하지 않는 시간대입니다");
        TITLES.put(ErrorCodeValue.ALERT_WINDOW_CLOSED, "이미 지난 시간대입니다");
        TITLES.put(ErrorCodeValue.ALERT_NOT_FOUND, "신청을 찾을 수 없습니다");
        TITLES.put(ErrorCodeValue.IDEMPOTENCY_KEY_REUSED, "이미 다른 요청에 쓰인 멱등 키입니다");
        TITLES.put(ErrorCodeValue.UPSTREAM_TIMEOUT, "예약 상태를 확인하지 못했습니다");
        TITLES.put(ErrorCodeValue.UPSTREAM_UNAVAILABLE, "예약 상태를 확인하지 못했습니다");
        TITLES.put(ErrorCodeValue.UPSTREAM_RESPONSE_UNREADABLE, "예약 상태를 확인하지 못했습니다");
        TITLES.put(ErrorCodeValue.STORAGE_TIMEOUT, "일시적으로 처리할 수 없습니다");
        TITLES.put(ErrorCodeValue.CONCURRENT_UPDATE_CONFLICT, "일시적으로 처리할 수 없습니다");
        TITLES.put(ErrorCodeValue.INTERNAL_ERROR, "요청을 처리하지 못했습니다");

        RETRY_AFTER.put(ErrorCodeValue.UPSTREAM_TIMEOUT, UPSTREAM_RETRY_AFTER_SECONDS);
        RETRY_AFTER.put(ErrorCodeValue.UPSTREAM_UNAVAILABLE, UPSTREAM_RETRY_AFTER_SECONDS);
        RETRY_AFTER.put(ErrorCodeValue.UPSTREAM_RESPONSE_UNREADABLE, UPSTREAM_RETRY_AFTER_SECONDS);
        RETRY_AFTER.put(ErrorCodeValue.STORAGE_TIMEOUT, CONTENTION_RETRY_AFTER_SECONDS);
        RETRY_AFTER.put(ErrorCodeValue.CONCURRENT_UPDATE_CONFLICT, CONTENTION_RETRY_AFTER_SECONDS);
        RETRY_AFTER.put(ErrorCodeValue.INTERNAL_ERROR, INTERNAL_RETRY_AFTER_SECONDS);
    }

    static ResponseEntity<Object> of(HttpStatus status, ErrorCodeValue code, String detail, String instance) {
        Integer retryAfterSeconds = RETRY_AFTER.get(code);
        boolean retryable = retryAfterSeconds != null;
        String traceId = status.is5xxServerError() ? newTraceId() : null;

        ProblemResponse problem = new ProblemResponse(
                TYPE_BASE + slug(code),
                TITLES.get(code),
                status.value(),
                detail,
                instance,
                code,
                retryable,
                retryAfterSeconds,
                traceId);

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON);
        if (retryable) {
            builder.header("Retry-After", String.valueOf(retryAfterSeconds));
        }
        return builder.body(body(problem));
    }

    /*
     * 선택 필드는 값이 없으면 키를 싣지 않는다. 계약은 detail·instance·retryAfterSeconds·traceId 를
     * 필수로 두지 않았고 널도 허용하지 않아서, 값이 없을 때 널을 실으면 그 필드의 타입을 어긴다.
     * 필수 필드는 언제나 값이 있으므로 이 규칙에 걸리지 않는다.
     */
    private static Map<String, Object> body(ProblemResponse problem) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", problem.type());
        body.put("title", problem.title());
        body.put("status", problem.status());
        putIfPresent(body, "detail", problem.detail());
        putIfPresent(body, "instance", problem.instance());
        body.put("code", problem.code());
        body.put("retryable", problem.retryable());
        putIfPresent(body, "retryAfterSeconds", problem.retryAfterSeconds());
        putIfPresent(body, "traceId", problem.traceId());
        return body;
    }

    private static void putIfPresent(Map<String, Object> body, String name, Object value) {
        if (value != null) {
            body.put(name, value);
        }
    }

    private static String slug(ErrorCodeValue code) {
        return code.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String newTraceId() {
        return UUID.randomUUID().toString();
    }

    private Problems() {
    }
}
