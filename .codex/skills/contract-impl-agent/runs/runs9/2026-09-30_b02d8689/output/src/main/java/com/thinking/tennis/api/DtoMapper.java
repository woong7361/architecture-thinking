package com.thinking.tennis.api;

import com.thinking.tennis.app.TennisAlertConstants;
import com.thinking.tennis.domain.Alert;
import com.thinking.tennis.domain.TimeSlot;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;

@Component
class DtoMapper {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final Clock clock = Clock.systemUTC();

    TimeSlot toTimeSlot(ApiDtos.TimeSlotDto dto) {
        if (dto == null || dto.startTime() == null || dto.endTime() == null) {
            throw new ApiValidationException("slot: startTime과 endTime이 필요합니다");
        }
        try {
            return new TimeSlot(LocalTime.parse(dto.startTime(), TIME_FORMATTER), LocalTime.parse(dto.endTime(), TIME_FORMATTER));
        } catch (DateTimeParseException ex) {
            throw new ApiValidationException("slot: HH:mm 형식이어야 합니다");
        }
    }

    ApiDtos.AlertDto toAlertDto(Alert alert) {
        return ApiDtos.AlertDto.from(alert, isCheckDelayed(alert));
    }

    private boolean isCheckDelayed(Alert alert) {
        Instant base = alert.lastCheckedAt() == null ? alert.createdAt() : alert.lastCheckedAt();
        return base.plus(TennisAlertConstants.CHECK_DELAY_THRESHOLD).isBefore(clock.instant());
    }
}
