package com.thinking.tennis.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.thinking.tennis.app.AvailabilityService;
import com.thinking.tennis.domain.DomainExceptions.AlertNotFound;
import com.thinking.tennis.domain.DomainExceptions.AlertWindowClosed;
import com.thinking.tennis.domain.DomainExceptions.ConcurrentUpdateConflict;
import com.thinking.tennis.domain.DomainExceptions.CourtNotSupported;
import com.thinking.tennis.domain.DomainExceptions.IdempotencyKeyReused;
import com.thinking.tennis.domain.DomainExceptions.SlotNotSupported;
import com.thinking.tennis.domain.DomainExceptions.StorageTimeout;
import com.thinking.tennis.port.CourtAvailabilityPort;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");

    @ExceptionHandler({
            RequestValidationException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Object> validation(Exception exception, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", false, null,
                "요청 값이 올바르지 않습니다", detail(exception), request);
    }

    @ExceptionHandler(ApiAuthentication.UnauthenticatedException.class)
    public ResponseEntity<Object> unauthenticated(HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", false, null,
                "인증이 필요합니다", null, request);
    }

    @ExceptionHandler(AvailabilityService.CourtNotSupported.class)
    public ResponseEntity<Object> pathCourtNotSupported(AvailabilityService.CourtNotSupported exception,
                                                          HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "COURT_NOT_SUPPORTED", false, null,
                "지원하지 않는 코트입니다", "courtId: " + exception.courtId(), request);
    }

    @ExceptionHandler(CourtNotSupported.class)
    public ResponseEntity<Object> bodyCourtNotSupported(CourtNotSupported exception, HttpServletRequest request) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, "COURT_NOT_SUPPORTED", false, null,
                "지원하지 않는 코트입니다", "courtId: " + exception.courtId(), request);
    }

    @ExceptionHandler(SlotNotSupported.class)
    public ResponseEntity<Object> slotNotSupported(HttpServletRequest request) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, "SLOT_NOT_SUPPORTED", false, null,
                "지원하지 않는 시간대입니다", null, request);
    }

    @ExceptionHandler(AlertWindowClosed.class)
    public ResponseEntity<Object> alertWindowClosed(HttpServletRequest request) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, "ALERT_WINDOW_CLOSED", false, null,
                "이미 지난 시간대입니다", null, request);
    }

    @ExceptionHandler(AlertNotFound.class)
    public ResponseEntity<Object> alertNotFound(HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "ALERT_NOT_FOUND", false, null,
                "신청을 찾을 수 없습니다", null, request);
    }

    @ExceptionHandler(IdempotencyKeyReused.class)
    public ResponseEntity<Object> idempotencyKeyReused(HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", false, null,
                "이미 다른 요청에 쓰인 멱등 키입니다", "같은 Idempotency-Key로 내용이 다른 요청이 앞서 처리됐습니다. 새 키로 보내십시오.", request);
    }

    @ExceptionHandler(ConcurrentUpdateConflict.class)
    public ResponseEntity<Object> concurrentUpdate(HttpServletRequest request) {
        return respond(HttpStatus.SERVICE_UNAVAILABLE, "CONCURRENT_UPDATE_CONFLICT", true, 1,
                "일시적으로 처리할 수 없습니다", "같은 신청을 동시에 고치려는 요청이 있었습니다. 같은 요청을 그대로 다시 보내면 됩니다.", request);
    }

    @ExceptionHandler(StorageTimeout.class)
    public ResponseEntity<Object> storageTimeout(HttpServletRequest request) {
        return respond(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_TIMEOUT", true, 1,
                "일시적으로 처리할 수 없습니다", null, request);
    }

    @ExceptionHandler(AvailabilityService.UpstreamAvailabilityFailure.class)
    public ResponseEntity<Object> upstreamFailure(AvailabilityService.UpstreamAvailabilityFailure exception,
                                                    HttpServletRequest request) {
        HttpStatus status = switch (exception.reason()) {
            case UPSTREAM_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case UPSTREAM_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case UPSTREAM_RESPONSE_UNREADABLE -> HttpStatus.BAD_GATEWAY;
        };
        return respond(status, exception.reason().name(), true, 20,
                "예약 상태를 확인하지 못했습니다", "예약처 확인에 실패했습니다. 빈자리가 없다는 뜻은 아닙니다.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> internalError(Exception exception, HttpServletRequest request) {
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", true, 5,
                "요청을 처리하지 못했습니다", null, request);
    }

    private ResponseEntity<Object> respond(HttpStatus status,
                                            String code,
                                            boolean retryable,
                                            Integer retryAfterSeconds,
                                            String title,
                                            String detail,
                                            HttpServletRequest request) {
        String traceId = status.is5xxServerError() ? UUID.randomUUID().toString().replace("-", "") : null;
        ApiModels.ProblemSpec problem = new ApiModels.ProblemSpec(
                "https://api.tennis-alert.example.com/problems/" + code.toLowerCase().replace('_', '-'),
                title,
                status.value(),
                code,
                retryable,
                retryAfterSeconds,
                detail);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(PROBLEM_JSON);
        if (retryAfterSeconds != null) {
            headers.set(HttpHeaders.RETRY_AFTER, Integer.toString(retryAfterSeconds));
        }
        return new ResponseEntity<>(problem.body(instancePath(request), traceId), headers, status);
    }

    private static String instancePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/v1") ? path : "/v1" + path;
    }

    private static String detail(Exception exception) {
        if (exception instanceof RequestValidationException
                || exception instanceof MissingServletRequestParameterException
                || exception instanceof MethodArgumentTypeMismatchException) {
            return exception.getMessage();
        }
        if (exception instanceof HttpMessageNotReadableException readable
                && readable.getCause() instanceof JsonProcessingException) {
            return "요청 본문을 읽을 수 없습니다";
        }
        return null;
    }
}
