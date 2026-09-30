package com.thinking.tennis.api;

import com.thinking.tennis.api.ApiDtos.ProblemResponse;
import com.thinking.tennis.app.ApplicationFailure;
import com.thinking.tennis.app.FailureCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApplicationFailure.class)
    public ResponseEntity<ProblemResponse> applicationFailure(
            ApplicationFailure failure,
            HttpServletRequest request
    ) {
        return problem(
                failure.code(),
                failure.status(),
                failure.retryable(),
                failure.retryAfterSeconds(),
                failure.getMessage(),
                request
        );
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ProblemResponse> validationFailure(
            Exception exception,
            HttpServletRequest request
    ) {
        return problem(
                FailureCode.VALIDATION_FAILED,
                400,
                false,
                null,
                "요청 값이 올바르지 않습니다",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemResponse> internalFailure(
            Exception exception,
            HttpServletRequest request
    ) {
        return problem(
                FailureCode.INTERNAL_ERROR,
                500,
                true,
                5,
                "요청을 처리하지 못했습니다",
                request
        );
    }

    private ResponseEntity<ProblemResponse> problem(
            FailureCode code,
            int status,
            boolean retryable,
            Integer retryAfterSeconds,
            String detail,
            HttpServletRequest request
    ) {
        String traceId = status >= 500 ? UUID.randomUUID().toString() : null;
        ProblemResponse body = new ProblemResponse(
                "https://api.tennis-alert.example.com/problems/" + code.name().toLowerCase(),
                title(code),
                status,
                detail,
                request.getRequestURI(),
                code,
                retryable,
                retryable ? retryAfterSeconds : null,
                traceId
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.valueOf("application/problem+json"));
        if (retryable && retryAfterSeconds != null) {
            headers.set("Retry-After", Integer.toString(retryAfterSeconds));
        }
        return new ResponseEntity<>(body, headers, HttpStatus.valueOf(status));
    }

    private String title(FailureCode code) {
        return switch (code) {
            case VALIDATION_FAILED -> "요청 값이 올바르지 않습니다";
            case UNAUTHENTICATED -> "인증이 필요합니다";
            case COURT_NOT_SUPPORTED -> "지원하지 않는 코트입니다";
            case SLOT_NOT_SUPPORTED -> "지원하지 않는 시간대입니다";
            case ALERT_WINDOW_CLOSED -> "이미 지난 시간대입니다";
            case ALERT_NOT_FOUND -> "신청을 찾을 수 없습니다";
            case IDEMPOTENCY_KEY_REUSED -> "이미 다른 요청에 쓰인 멱등 키입니다";
            default -> "요청을 처리하지 못했습니다";
        };
    }
}
