package com.thinking.tennis.api.dto;

import com.thinking.tennis.domain.CourtDayAvailability;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 계약의 {@code AvailabilitySlot} 이다.
 */
@Schema(name = "AvailabilitySlot", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record AvailabilitySlotResponse(

        @Schema(type = "string",
                pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$",
                example = "06:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String startTime,

        @Schema(type = "string",
                pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$",
                example = "08:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String endTime,

        @Schema(description = """
                이 시간대가 마지막 확인 시점에 비어 있었는지 여부다. 확인에 실패한 경우의 값이 아니다.
                확인 실패는 `stale`로 드러나며, `stale`이 true이면 이 값은 `confirmedAt` 시점의 사실이다.
                """,
                type = "boolean",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean available) {

    public static AvailabilitySlotResponse of(CourtDayAvailability.SlotStatus status) {
        return new AvailabilitySlotResponse(
                Payloads.time(status.slot().startTime()),
                Payloads.time(status.slot().endTime()),
                status.available());
    }
}
