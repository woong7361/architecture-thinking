package com.thinking.tennis.api;

import com.thinking.tennis.app.FailureCode;
import com.thinking.tennis.app.FailureContext;
import com.thinking.tennis.app.UseCaseException;
import com.thinking.tennis.domain.AlertStatus;
import com.thinking.tennis.domain.TimeSlot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

final class RequestParsing {
    private static final String UUID_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private RequestParsing() {
    }

    static UUID uuid(String value, String field) {
        if (value == null || !value.matches(UUID_PATTERN)) {
            throw validation(field + ": UUID 형식이어야 합니다");
        }
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw validation(field + ": UUID 형식이어야 합니다");
        }
    }

    static LocalDate date(String value, String field) {
        if (value == null || value.isBlank()) {
            throw validation(field + ": 필수 값입니다");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw validation(field + ": 날짜 형식이어야 합니다");
        }
    }

    static TimeSlot slot(TimeSlotDto dto) {
        if (dto == null || dto.startTime() == null || dto.endTime() == null) {
            throw validation("slot: 필수 객체입니다");
        }
        if (!dto.startTime().matches("^([01][0-9]|2[0-3]):[0-5][0-9]$")
                || !dto.endTime().matches("^([01][0-9]|2[0-3]):[0-5][0-9]$")) {
            throw validation("slot: HH:mm 형식이어야 합니다");
        }
        return new TimeSlot(LocalTime.parse(dto.startTime()), LocalTime.parse(dto.endTime()));
    }

    static AlertStatus status(String value) {
        try {
            return AlertStatus.valueOf(value);
        } catch (RuntimeException exception) {
            throw validation("status: 알 수 없는 상태입니다");
        }
    }

    static void required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw validation(field + ": 필수 값입니다");
        }
    }

    static UseCaseException validation(String detail) {
        return new UseCaseException(
                FailureCode.VALIDATION_FAILED,
                FailureContext.GENERIC,
                detail,
                false,
                null
        );
    }
}
