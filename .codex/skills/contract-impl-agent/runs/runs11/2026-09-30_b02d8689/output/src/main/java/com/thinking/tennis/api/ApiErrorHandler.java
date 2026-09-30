package com.thinking.tennis.api;

import com.thinking.tennis.api.dto.ErrorCode;
import com.thinking.tennis.api.dto.Problem;
import com.thinking.tennis.app.AlertOperationFailure;
import com.thinking.tennis.app.AvailabilityPolicy;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.UUID;

/**
 * 실패를 계약이 적은 상태 코드와 본문으로 옮긴다.
 *
 * <p>실패를 상태 코드로 옮기는 판단이 이 한 자리에 모여 있다. 유스케이스와 포트는 무엇이 안 됐는지만
 * 말하고, 어느 상태 코드로 나가는지는 계약이 정한 것이므로 계약을 읽는 자리가 정해야 한다.
 *
 * <p>4xx와 5xx의 모양이 다르다. 4xx는 요청을 고치면 풀리므로 재시도 가능하지 않다고 적고 서버 로그
 * 식별자를 싣지 않는다. 5xx는 재시도 가능하다고 적고 기다릴 초와 로그 식별자를 함께 싣는다. 재시도
 * 가능하다고 적으면 기다릴 초를 본문과 헤더에 반드시 함께 보낸다.
 */
@RestControllerAdvice
public class ApiErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorHandler.class);

    /** 저장소와 경합으로 물러난 실패가 권하는 대기 시간이다. */
    private static final int SHORT_RETRY_SECONDS = 1;

    /** 내부 실패가 권하는 대기 시간이다. */
    private static final int INTERNAL_RETRY_SECONDS = 5;

    /** 예약처 확인 실패가 권하는 대기 시간이다. 계약이 확인 간격을 따르라고 적었다. */
    private static final int UPSTREAM_RETRY_SECONDS = (int) AvailabilityPolicy.CHECK_INTERVAL.toSeconds();

    @ExceptionHandler(ApiFailure.ValidationFailed.class)
    public ResponseEntity<Map<String, Object>> validationFailed(ApiFailure.ValidationFailed failure,
                                                                HttpServletRequest request) {
        return clientError(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, failure.getMessage(), request);
    }

    @ExceptionHandler(ApiFailure.Unauthenticated.class)
    public ResponseEntity<Map<String, Object>> unauthenticated(HttpServletRequest request) {
        return clientError(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED, null, request);
    }

    @ExceptionHandler(ApiFailure.IdempotencyKeyReused.class)
    public ResponseEntity<Map<String, Object>> idempotencyKeyReused(HttpServletRequest request) {
        return clientError(HttpStatus.CONFLICT, ErrorCode.IDEMPOTENCY_KEY_REUSED,
                "같은 Idempotency-Key로 내용이 다른 요청이 앞서 처리됐습니다. 새 키로 보내십시오.", request);
    }

    @ExceptionHandler(ApiFailure.RequestInProgress.class)
    public ResponseEntity<Map<String, Object>> requestInProgress(HttpServletRequest request) {
        return serverError(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.CONCURRENT_UPDATE_CONFLICT,
                "같은 Idempotency-Key의 앞선 요청이 아직 처리 중입니다. 잠시 뒤 같은 키로 다시 보내면 그 결과를 받습니다.",
                SHORT_RETRY_SECONDS, request);
    }

    /**
     * 지원하지 않는 코트다. 경로에서 받았으면 404, 본문에서 받았으면 422로 나간다. 경로가 가리킨 자원이
     * 없는 것과 본문이 가리킨 조건을 받을 수 없는 것은 클라이언트가 고칠 자리가 다르다.
     */
    @ExceptionHandler(AlertOperationFailure.UnsupportedCourt.class)
    public ResponseEntity<Map<String, Object>> unsupportedCourt(AlertOperationFailure.UnsupportedCourt failure,
                                                                HttpServletRequest request) {
        HttpStatus status = failure.origin() == AlertOperationFailure.Origin.PATH
                ? HttpStatus.NOT_FOUND
                : HttpStatus.UNPROCESSABLE_ENTITY;
        return clientError(status, ErrorCode.COURT_NOT_SUPPORTED, "courtId: " + failure.courtId(), request);
    }

    @ExceptionHandler(AlertOperationFailure.UnsupportedSlot.class)
    public ResponseEntity<Map<String, Object>> unsupportedSlot(AlertOperationFailure.UnsupportedSlot failure,
                                                               HttpServletRequest request) {
        return clientError(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.SLOT_NOT_SUPPORTED,
                failure.description(), request);
    }

    @ExceptionHandler(AlertOperationFailure.WindowClosed.class)
    public ResponseEntity<Map<String, Object>> windowClosed(HttpServletRequest request) {
        return clientError(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.ALERT_WINDOW_CLOSED,
                "이용 시작 시각이 지나 감시할 수 없습니다", request);
    }

    @ExceptionHandler(AlertOperationFailure.AlertNotFound.class)
    public ResponseEntity<Map<String, Object>> alertNotFound(HttpServletRequest request) {
        return clientError(HttpStatus.NOT_FOUND, ErrorCode.ALERT_NOT_FOUND, null, request);
    }

    /**
     * 예약처 확인이 실패했고 저장된 마지막 성공 결과도 없다. 실패 이유마다 다른 상태 코드로 나가는 것은
     * 계약이 그렇게 적었기 때문이다. 어떤 실패였는지 본문으로만 가르면 5xx를 상태 코드로 다루는 중간
     * 경로가 세 실패를 구별하지 못한다.
     */
    @ExceptionHandler(AlertOperationFailure.AvailabilityUnavailable.class)
    public ResponseEntity<Map<String, Object>> availabilityUnavailable(
            AlertOperationFailure.AvailabilityUnavailable failure, HttpServletRequest request) {
        return switch (failure.reason()) {
            case UPSTREAM_TIMEOUT -> serverError(HttpStatus.GATEWAY_TIMEOUT, ErrorCode.UPSTREAM_TIMEOUT,
                    "예약처가 제때 응답하지 않았습니다. 빈자리가 없다는 뜻은 아닙니다.",
                    UPSTREAM_RETRY_SECONDS, request);
            case UPSTREAM_UNAVAILABLE -> serverError(HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.UPSTREAM_UNAVAILABLE,
                    "예약처에 연결하지 못했습니다. 빈자리가 없다는 뜻은 아닙니다.",
                    UPSTREAM_RETRY_SECONDS, request);
            case UPSTREAM_RESPONSE_UNREADABLE -> serverError(HttpStatus.BAD_GATEWAY,
                    ErrorCode.UPSTREAM_RESPONSE_UNREADABLE,
                    "예약처 응답을 읽지 못했습니다. 빈자리가 없다는 뜻은 아닙니다.",
                    UPSTREAM_RETRY_SECONDS, request);
        };
    }

    /**
     * 신청 조건을 판정할 근거를 얻지 못했다. 신청 오퍼레이션에는 예약처 실패를 알리는 상태 코드가 없어서
     * 계약이 둔 내부 실패로 나간다. 이 오퍼레이션은 멱등 키를 요구하므로 같은 요청을 그대로 다시 보내도
     * 안전하다.
     */
    @ExceptionHandler(AlertOperationFailure.AvailabilityUndetermined.class)
    public ResponseEntity<Map<String, Object>> availabilityUndetermined(
            AlertOperationFailure.AvailabilityUndetermined failure, HttpServletRequest request) {
        log.warn("운영 시간대를 확인하지 못해 신청 조건을 판정할 수 없습니다: {}", failure.reason());
        return serverError(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, null,
                INTERNAL_RETRY_SECONDS, request);
    }

    @ExceptionHandler(AlertOperationFailure.ConcurrentUpdate.class)
    public ResponseEntity<Map<String, Object>> concurrentUpdate(HttpServletRequest request) {
        return serverError(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.CONCURRENT_UPDATE_CONFLICT,
                "같은 신청을 동시에 고치려는 요청이 있었습니다. 같은 요청을 그대로 다시 보내면 됩니다.",
                SHORT_RETRY_SECONDS, request);
    }

    /** 필수 헤더가 없다. 계약은 멱등 키 헤더의 누락도 요청 형식 위반으로 다루라고 적었다. */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> missingHeader(MissingRequestHeaderException failure,
                                                             HttpServletRequest request) {
        String detail = "Idempotency-Key".equalsIgnoreCase(failure.getHeaderName())
                ? "Idempotency-Key: 필수 헤더입니다. 재시도 안전을 보장하기 위해 UUID 형식의 값을 요구합니다"
                : failure.getHeaderName() + ": 필수 헤더입니다";
        return clientError(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, detail, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> missingParameter(MissingServletRequestParameterException failure,
                                                                HttpServletRequest request) {
        return clientError(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
                failure.getParameterName() + ": 필수 값입니다", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> typeMismatch(MethodArgumentTypeMismatchException failure,
                                                            HttpServletRequest request) {
        return clientError(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
                failure.getName() + ": 값의 형식이 올바르지 않습니다", request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<Map<String, Object>> unreadableBody(HttpServletRequest request) {
        return clientError(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
                "본문을 읽지 못했습니다. application/json 으로 계약의 스키마를 만족하는 본문을 보내십시오", request);
    }

    /**
     * 위 어디에도 해당하지 않는 실패다.
     *
     * <p>프레임워크가 요청 자체를 거절한 4xx는 계약이 둔 요청 형식 위반으로 옮긴다. 그 밖의 것은 서버
     * 내부 실패이므로 로그 식별자를 함께 남긴다. 사용자가 실패를 신고할 때 그 값으로 로그를 찾는다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception failure, HttpServletRequest request) {
        if (failure instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            return clientError(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
                    "요청을 처리할 수 없는 형식입니다", request);
        }
        String traceId = newTraceId();
        log.error("요청을 처리하지 못했습니다. traceId={}", traceId, failure);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR,
                new Problem(ErrorCode.INTERNAL_ERROR.problemType(), ErrorCode.INTERNAL_ERROR.title(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(), null, ApiUris.instance(request),
                        ErrorCode.INTERNAL_ERROR, true, INTERNAL_RETRY_SECONDS, traceId),
                INTERNAL_RETRY_SECONDS);
    }

    private ResponseEntity<Map<String, Object>> clientError(HttpStatus status, ErrorCode code,
                                                            String detail, HttpServletRequest request) {
        return respond(status, new Problem(code.problemType(), code.title(), status.value(), detail,
                ApiUris.instance(request), code, false, null, null), null);
    }

    private ResponseEntity<Map<String, Object>> serverError(HttpStatus status, ErrorCode code, String detail,
                                                            int retryAfterSeconds, HttpServletRequest request) {
        String traceId = newTraceId();
        log.warn("요청을 처리하지 못했습니다. code={} traceId={}", code, traceId);
        return respond(status, new Problem(code.problemType(), code.title(), status.value(), detail,
                ApiUris.instance(request), code, true, retryAfterSeconds, traceId), retryAfterSeconds);
    }

    private ResponseEntity<Map<String, Object>> respond(HttpStatus status, Problem problem,
                                                        Integer retryAfterSeconds) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON);
        if (retryAfterSeconds != null) {
            builder.header(HttpHeaders.RETRY_AFTER, Integer.toString(retryAfterSeconds));
        }
        return builder.body(problem.toBody());
    }

    private static String newTraceId() {
        return UUID.randomUUID().toString();
    }
}
