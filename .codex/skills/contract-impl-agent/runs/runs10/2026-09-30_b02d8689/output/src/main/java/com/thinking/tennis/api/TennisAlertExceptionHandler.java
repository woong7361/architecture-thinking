package com.thinking.tennis.api;

import com.thinking.tennis.app.UseCaseException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice
public class TennisAlertExceptionHandler {
    @ExceptionHandler(UseCaseException.class)
    public ResponseEntity<ProblemResponse> handleUseCase(UseCaseException exception) {
        return problem(exception.status(), exception.code(), exception.getMessage(), exception.retryable(),
                exception.retryAfterSeconds());
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            MissingRequestHeaderException.class
    })
    public ResponseEntity<ProblemResponse> handleValidation(Exception exception) {
        return problem(400, "VALIDATION_FAILED", exception.getMessage(), false, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemResponse> handleUnexpected(Exception exception) {
        return problem(500, "INTERNAL_ERROR", "The request could not be processed.", true, 5);
    }

    private ResponseEntity<ProblemResponse> problem(int status,
                                                     String code,
                                                     String detail,
                                                     boolean retryable,
                                                     Integer retryAfterSeconds) {
        String type = "https://api.tennis-alert.example.com/problems/" + code.toLowerCase().replace('_', '-');
        String traceId = status >= 500 ? UUID.randomUUID().toString() : null;
        ProblemResponse body = new ProblemResponse(type, title(code), status, detail, null, code, retryable,
                retryAfterSeconds, traceId);
        HttpHeaders headers = new HttpHeaders();
        if (retryable && retryAfterSeconds != null) {
            headers.set("Retry-After", String.valueOf(retryAfterSeconds));
        }
        return new ResponseEntity<>(body, headers, HttpStatus.valueOf(status));
    }

    private static String title(String code) {
        return switch (code) {
            case "VALIDATION_FAILED" -> "Invalid request";
            case "UNAUTHENTICATED" -> "Authentication required";
            case "COURT_NOT_SUPPORTED" -> "Court is not supported";
            case "SLOT_NOT_SUPPORTED" -> "Slot is not supported";
            case "ALERT_WINDOW_CLOSED" -> "Alert window is closed";
            case "ALERT_NOT_FOUND" -> "Alert not found";
            case "IDEMPOTENCY_KEY_REUSED" -> "Idempotency key was reused";
            case "CONCURRENT_UPDATE_CONFLICT" -> "Concurrent update conflict";
            case "UPSTREAM_TIMEOUT", "UPSTREAM_UNAVAILABLE", "UPSTREAM_RESPONSE_UNREADABLE" ->
                    "Availability could not be checked";
            case "STORAGE_TIMEOUT" -> "Temporarily unavailable";
            default -> "Request could not be processed";
        };
    }
}
