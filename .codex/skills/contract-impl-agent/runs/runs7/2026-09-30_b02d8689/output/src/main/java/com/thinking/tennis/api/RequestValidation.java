package com.thinking.tennis.api;

import com.thinking.tennis.app.AlertCommand;
import com.thinking.tennis.app.ApplicationFailure;
import com.thinking.tennis.app.FailureCode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public final class RequestValidation {
    private static final Pattern UUID_TEXT = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    private static final Pattern COURT_ID = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");
    private static final Pattern TIME_TEXT = Pattern.compile("^([01][0-9]|2[0-3]):[0-5][0-9]$");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private RequestValidation() {
    }

    public static String uuid(String value, String field) {
        if (value == null || !UUID_TEXT.matcher(value).matches()) {
            throw invalid(field + ": UUID 형식이어야 합니다");
        }
        return value;
    }

    public static String courtId(String value, String field) {
        if (value == null || !COURT_ID.matcher(value).matches()) {
            throw invalid(field + ": 코트 식별자 형식이어야 합니다");
        }
        return value;
    }

    public static LocalDate date(String value, String field) {
        try {
            if (value == null) {
                throw new DateTimeParseException("null", "", 0);
            }
            return LocalDate.parse(value, DATE_FORMAT);
        } catch (DateTimeParseException exception) {
            throw invalid(field + ": 날짜 형식이어야 합니다");
        }
    }

    public static LocalTime time(String value, String field) {
        if (value == null || !TIME_TEXT.matcher(value).matches()) {
            throw invalid(field + ": HH:mm 형식이어야 합니다");
        }
        try {
            return LocalTime.parse(value, TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            throw invalid(field + ": HH:mm 형식이어야 합니다");
        }
    }

    public static AlertCommand alertCommand(ApiDtos.AlertRequest request) {
        if (request == null || request.slot() == null) {
            throw invalid("slot: 필수입니다");
        }
        return new AlertCommand(
                courtId(request.courtId(), "courtId"),
                date(request.date(), "date"),
                time(request.slot().startTime(), "slot.startTime"),
                time(request.slot().endTime(), "slot.endTime")
        );
    }

    private static ApplicationFailure invalid(String detail) {
        return ApplicationFailure.of(FailureCode.VALIDATION_FAILED, 400, false, null, detail);
    }
}
