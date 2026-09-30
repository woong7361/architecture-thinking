package com.thinking.tennis.api;

import com.thinking.tennis.api.dto.ErrorCodeValue;
import com.thinking.tennis.app.error.AlertNotFoundException;
import com.thinking.tennis.app.error.AlertWindowClosedException;
import com.thinking.tennis.app.error.AvailabilityUnavailableException;
import com.thinking.tennis.app.error.ConcurrentUpdateException;
import com.thinking.tennis.app.error.IdempotencyKeyReusedException;
import com.thinking.tennis.app.error.ScheduleUnknownException;
import com.thinking.tennis.app.error.SlotNotSupportedException;
import com.thinking.tennis.app.error.StorageTimeoutException;
import com.thinking.tennis.app.error.UnsupportedCourtException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 실패를 계약이 정한 상태 코드와 에러 코드로 옮긴다.
 *
 * <p>안쪽은 실패를 상태 코드로 말하지 않는다. 어떤 실패가 어떤 상태 코드로 나가는지는 계약이 정하고
 * 그것을 읽는 자리는 요청을 받은 이곳이다.
 *
 * <p>이 자리를 스펙에서 감추는 이유는, 추출기가 예외 처리기를 보고 모든 오퍼레이션에 응답을 덧붙이면
 * 각 오퍼레이션이 선언한 실패 목록이 계약과 달라지기 때문이다. 어떤 오퍼레이션이 어떤 실패를 내는지는
 * 오퍼레이션마다 계약이 정한 대로 선언했고, 그 목록이 판정의 대상이다.
 */
@RestControllerAdvice
@Hidden
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(UnauthenticatedException.class)
    ResponseEntity<Object> handleUnauthenticated(UnauthenticatedException e, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, ErrorCodeValue.UNAUTHENTICATED, null, request);
    }

    @ExceptionHandler(RequestValidationException.class)
    ResponseEntity<Object> handleRequestValidation(RequestValidationException e, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCodeValue.VALIDATION_FAILED, e.getMessage(), request);
    }

    /**
     * 스프링이 요청을 값으로 옮기다 실패한 경우다.
     *
     * <p>본문을 읽지 못한 것과 필수 질의 항목·헤더가 빠진 것과 타입이 맞지 않는 것을 한 자리에서 다룬다.
     * 계약이 이 넷에 같은 코드를 쓰라고 했고, 클라이언트의 대응도 요청을 고치는 것으로 같다.
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class,
            ServletRequestBindingException.class,
            MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<Object> handleBinding(Exception e, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCodeValue.VALIDATION_FAILED,
                "요청을 읽지 못했습니다. 계약이 정한 형식으로 보내십시오", request);
    }

    @ExceptionHandler(UnsupportedCourtException.class)
    ResponseEntity<Object> handleUnsupportedCourt(UnsupportedCourtException e, HttpServletRequest request) {
        HttpStatus status = e.source() == UnsupportedCourtException.Source.PATH
                ? HttpStatus.NOT_FOUND
                : HttpStatus.UNPROCESSABLE_ENTITY;
        return problem(status, ErrorCodeValue.COURT_NOT_SUPPORTED, "courtId: " + e.courtId(), request);
    }

    /**
     * 그 코트가 그 날짜에 운영하지 않는 시간대로 신청한 경우다.
     *
     * <p>설명에 운영하는 시간대를 함께 적는다. 사용자가 시간대를 고쳐 다시 신청하려면 어떤 시간대가
     * 되는지를 알아야 하고, 안 되는 것만 알려 주면 예약 상태 조회를 한 번 더 부르게 된다.
     */
    @ExceptionHandler(SlotNotSupportedException.class)
    ResponseEntity<Object> handleSlotNotSupported(SlotNotSupportedException e, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCodeValue.SLOT_NOT_SUPPORTED,
                slotDetail(e), request);
    }

    /*
     * 운영 시간대 목록이 비어 있으면 이 실패가 나지 않는다. 빈 목록은 그 코트가 그 날짜에 아무 시간대도
     * 운영하지 않는다는 뜻이 아니라 예약처가 아직 그 날짜를 열지 않았다는 뜻이고, 계약은 그때 시간대
     * 판정을 미루고 신청을 받으라고 한다. 아직 열리지 않은 날짜를 운영하지 않는 시간대로 거절하면
     * 사용자는 나중에 열릴 날짜를 포기한다.
     */
    private static String slotDetail(SlotNotSupportedException e) {
        String operating = e.operatingSlots().stream()
                .map(slot -> slot.startTime() + "~" + slot.endTime())
                .collect(Collectors.joining(", "));
        return e.courtId() + "은 " + e.date() + "에 " + operating + " 단위로 운영합니다";
    }

    @ExceptionHandler(AlertWindowClosedException.class)
    ResponseEntity<Object> handleWindowClosed(AlertWindowClosedException e, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCodeValue.ALERT_WINDOW_CLOSED,
                e.getMessage(), request);
    }

    @ExceptionHandler(AlertNotFoundException.class)
    ResponseEntity<Object> handleAlertNotFound(AlertNotFoundException e, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ErrorCodeValue.ALERT_NOT_FOUND, null, request);
    }

    @ExceptionHandler(IdempotencyKeyReusedException.class)
    ResponseEntity<Object> handleIdempotencyKeyReused(IdempotencyKeyReusedException e, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ErrorCodeValue.IDEMPOTENCY_KEY_REUSED,
                "같은 Idempotency-Key로 내용이 다른 요청이 앞서 처리됐습니다. 새 키로 보내십시오.", request);
    }

    @ExceptionHandler(ConcurrentUpdateException.class)
    ResponseEntity<Object> handleConcurrentUpdate(ConcurrentUpdateException e, HttpServletRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, ErrorCodeValue.CONCURRENT_UPDATE_CONFLICT,
                e.getMessage(), request);
    }

    @ExceptionHandler(StorageTimeoutException.class)
    ResponseEntity<Object> handleStorageTimeout(StorageTimeoutException e, HttpServletRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, ErrorCodeValue.STORAGE_TIMEOUT, null, request);
    }

    /**
     * 확인이 실패했고 저장된 성공 결과도 없는 경우다.
     *
     * <p>실패한 이유마다 상태 코드가 다르다. 시간 안에 응답이 없었으면 504, 닿지 못했으면 503, 응답을
     * 읽지 못했으면 502다. 어느 쪽이든 빈자리가 없다는 뜻이 아니므로 그 사실을 설명에 적는다.
     */
    @ExceptionHandler(AvailabilityUnavailableException.class)
    ResponseEntity<Object> handleAvailabilityUnavailable(AvailabilityUnavailableException e,
                                                        HttpServletRequest request) {
        return switch (e.reason()) {
            case UPSTREAM_TIMEOUT -> problem(HttpStatus.GATEWAY_TIMEOUT, ErrorCodeValue.UPSTREAM_TIMEOUT,
                    "예약처가 제때 응답하지 않았습니다. 빈자리가 없다는 뜻은 아닙니다.", request);
            case UPSTREAM_UNAVAILABLE -> problem(HttpStatus.SERVICE_UNAVAILABLE, ErrorCodeValue.UPSTREAM_UNAVAILABLE,
                    "예약처에 연결하지 못했습니다. 빈자리가 없다는 뜻은 아닙니다.", request);
            case UPSTREAM_RESPONSE_UNREADABLE -> problem(HttpStatus.BAD_GATEWAY,
                    ErrorCodeValue.UPSTREAM_RESPONSE_UNREADABLE,
                    "예약처 응답을 읽지 못했습니다. 빈자리가 없다는 뜻은 아닙니다.", request);
        };
    }

    /**
     * 신청 조건을 판정할 재료를 얻지 못한 경우다.
     *
     * <p>운영 시간대의 출처는 그 코트·날짜의 확인 결과 하나뿐이고, 확인이 실패했고 저장된 성공 결과도
     * 없으면 조건을 판정할 수 없다. 계약이 이 실패를 예약 상태 조회와 같은 조건으로 알리라고 하므로
     * 실패한 이유마다 같은 상태 코드와 에러 코드로 나간다. 서버 결함이 아니라 예약처에 닿지 못한 것이고,
     * 다시 시도하기까지 기다릴 초도 확인 간격을 따른다.
     */
    @ExceptionHandler(ScheduleUnknownException.class)
    ResponseEntity<Object> handleScheduleUnknown(ScheduleUnknownException e, HttpServletRequest request) {
        log.warn("운영 시간대를 확인하지 못해 신청을 판정할 수 없습니다", e);
        return switch (e.reason()) {
            case UPSTREAM_TIMEOUT -> problem(HttpStatus.GATEWAY_TIMEOUT, ErrorCodeValue.UPSTREAM_TIMEOUT,
                    "예약처가 제때 응답하지 않아 운영 시간대를 확인하지 못했습니다. 빈자리가 없다는 뜻은 아닙니다.", request);
            case UPSTREAM_UNAVAILABLE -> problem(HttpStatus.SERVICE_UNAVAILABLE, ErrorCodeValue.UPSTREAM_UNAVAILABLE,
                    "예약처에 연결하지 못해 운영 시간대를 확인하지 못했습니다. 빈자리가 없다는 뜻은 아닙니다.", request);
            case UPSTREAM_RESPONSE_UNREADABLE -> problem(HttpStatus.BAD_GATEWAY,
                    ErrorCodeValue.UPSTREAM_RESPONSE_UNREADABLE,
                    "예약처 응답을 읽지 못해 운영 시간대를 확인하지 못했습니다. 빈자리가 없다는 뜻은 아닙니다.", request);
        };
    }

    /**
     * 본문의 미디어 타입을 다룰 수 없는 경우다.
     *
     * <p>스프링은 이 실패를 프로토콜 수준에서 415로 되돌리지만, 계약은 이 오퍼레이션의 실패를 스스로
     * 선언한 목록으로 닫았고 요청의 형식 위반을 {@code VALIDATION_FAILED} 로 오게 했다. 그 목록에 없는
     * 상태 코드에 실패 응답의 모양도 아닌 본문을 내보내면, 모든 실패에 코드가 있다고 읽고 만든
     * 클라이언트가 그 자리에서 깨진다.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<Object> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e,
                                                      HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCodeValue.VALIDATION_FAILED,
                "Content-Type: 본문은 application/json으로 보내십시오", request);
    }

    /**
     * 위 어디에도 해당하지 않는 서버 내부 실패다.
     *
     * <p>검사 예외를 여기서 받지 않는 이유는 스프링이 프로토콜 수준으로 다루는 실패(허용하지 않는 메서드,
     * 받아들일 수 없는 응답 형식)를 내부 실패로 바꾸지 않기 위해서다. 그 실패들은 계약이 선언한 것이 아니고
     * 요청 줄과 헤더를 고쳐야 풀린다. 본문의 미디어 타입만 예외로 위에서 따로 받는다. 그것은 본문을 값으로
     * 옮기는 일의 실패이고 계약이 그 자리에 코드를 두었다.
     */
    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<Object> handleUnexpected(RuntimeException e, HttpServletRequest request) {
        log.error("처리하지 못한 실패", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodeValue.INTERNAL_ERROR, null, request);
    }

    private static ResponseEntity<Object> problem(HttpStatus status,
                                                  ErrorCodeValue code,
                                                  String detail,
                                                  HttpServletRequest request) {
        return Problems.of(status, code, detail, ApiPaths.instance(request.getRequestURI()));
    }
}
