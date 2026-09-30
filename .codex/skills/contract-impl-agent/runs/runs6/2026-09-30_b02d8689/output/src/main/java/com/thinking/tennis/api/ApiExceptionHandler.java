package com.thinking.tennis.api;

import com.thinking.tennis.app.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiModels.Problem> handleApplicationException(
            ApplicationException exception, HttpServletRequest request) {
        return problemResponse(exception, request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiModels.Problem> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        ApplicationException internal = ApplicationException.internalError();
        return problemResponse(internal, request.getRequestURI());
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiModels.Problem> handleBadRequest(
            Exception exception, HttpServletRequest request) {
        return problemResponse(
                ApplicationException.validation("요청 값이 올바르지 않습니다"),
                request.getRequestURI());
    }

    private ResponseEntity<ApiModels.Problem> problemResponse(
            ApplicationException exception, String instance) {
        ApiModels.Problem problem = new ApiModels.Problem(
                "https://api.tennis-alert.example.com/problems/" + exception.code().name().toLowerCase().replace('_', '-'),
                exception.title(),
                exception.status().value(),
                exception.detail(),
                instance,
                exception.code(),
                exception.retryable(),
                exception.retryAfterSeconds(),
                exception.status().is5xxServerError() ? UUID.randomUUID().toString() : null);

        HttpHeaders headers = new HttpHeaders();
        if (exception.retryable() && exception.retryAfterSeconds() != null) {
headers.set("Retry-After", String.valueOf(exception.retryAfterSeconds()));
        }
        return new ResponseEntity<>(problem, headers, exception.status());
    }
}
