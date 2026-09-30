package com.thinking.tennis.api;

import com.thinking.tennis.app.UseCaseException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public final class RequestValidation {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private RequestValidation() {
    }

    public static LocalDate date(String value) {
        try {
            if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new DateTimeParseException("invalid date", value == null ? "" : value, 0);
            }
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw invalid("date");
        }
    }

    public static LocalTime time(String value, String field) {
        try {
            if (value == null || !value.matches("([01][0-9]|2[0-3]):[0-5][0-9]")) {
                throw new DateTimeParseException("invalid time", value == null ? "" : value, 0);
            }
            return LocalTime.parse(value, TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            throw invalid(field);
        }
    }

    public static java.util.UUID alertId(String value) {
        try {
            return java.util.UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw invalid("alertId");
        }
    }

    public static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid(field);
        }
    }

    private static UseCaseException invalid(String field) {
        return new UseCaseException(
                UseCaseException.Code.VALIDATION_FAILED,
                field + ": 요청 값이 올바르지 않습니다");
    }
}
