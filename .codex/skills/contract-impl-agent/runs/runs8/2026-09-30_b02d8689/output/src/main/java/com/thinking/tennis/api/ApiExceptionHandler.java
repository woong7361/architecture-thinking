package com.thinking.tennis.api;

import com.thinking.tennis.app.ApiFailure;
import com.thinking.tennis.app.AvailabilityFailure;
import com.thinking.tennis.app.ValidationFailure;
import com.thinking.tennis.port.CourtAvailabilityPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ValidationFailure.class)
    public ResponseEntity<ApiModels.Problem> validation(ValidationFailure failure) {
        return problem(400, "VALIDATION_FAILED", false, null, failure.getMessage(), null);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiModels.Problem> binding(Exception failure) {
        return problem(400, "VALIDATION_FAILED", false, null, "The request does not satisfy the contract.", null);
    }

    @ExceptionHandler(ApiFailure.class)
    public ResponseEntity<ApiModels.Problem> apiFailure(ApiFailure failure) {
        String traceId = failure.status() >= 500 ? UUID.randomUUID().toString() : null;
        return problem(
                failure.status(),
                failure.code(),
                failure.retryable(),
                failure.retryAfterSeconds(),
                failure.detail(),
                traceId);
    }

    @ExceptionHandler(AvailabilityFailure.class)
    public ResponseEntity<ApiModels.Problem> availabilityFailure(AvailabilityFailure failure) {
        int status = switch (failure.reason()) {
            case UPSTREAM_TIMEOUT -> 504;
            case UPSTREAM_RESPONSE_UNREADABLE -> 502;
            case UPSTREAM_UNAVAILABLE -> 503;
        };
        String traceId = UUID.randomUUID().toString();
        return problem(status, failure.reason().name(), true, 20,
                "Availability could not be confirmed. This does not mean the slot is unavailable.", traceId);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiModels.Problem> unexpected(Exception failure) {
        return problem(500, "INTERNAL_ERROR", true, 5,
                "The request could not be processed.", UUID.randomUUID().toString());
    }

    private ResponseEntity<ApiModels.Problem> problem(
            int status,
            String code,
            boolean retryable,
            Integer retryAfterSeconds,
            String detail,
            String traceId
    ) {
        ApiModels.Problem body = new ApiModels.Problem(
                "https://api.tennis-alert.example.com/problems/" + code.toLowerCase(),
                title(code),
                status,
                detail,
                null,
                code,
                retryable,
                retryable ? retryAfterSeconds : null,
                status >= 500 ? traceId : null);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status)
                .contentType(MediaType.valueOf("application/problem+json"));
        if (retryable && retryAfterSeconds != null) {
            response.header(HttpHeaders.RETRY_AFTER, Integer.toString(retryAfterSeconds));
        }
        return response.body(body);
    }

    private String title(String code) {
        return switch (code) {
            case "UNAUTHENTICATED" -> "Authentication is required.";
            case "ALERT_NOT_FOUND" -> "Alert was not found.";
            case "COURT_NOT_SUPPORTED" -> "Court is not supported.";
            case "SLOT_NOT_SUPPORTED" -> "Slot is not supported.";
            case "ALERT_WINDOW_CLOSED" -> "The alert window is closed.";
            default -> "The request could not be processed.";
        };
    }
}
