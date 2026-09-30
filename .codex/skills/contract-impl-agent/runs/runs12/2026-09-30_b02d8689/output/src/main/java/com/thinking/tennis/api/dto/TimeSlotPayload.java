package com.thinking.tennis.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 계약의 {@code TimeSlot} 이다.
 */
@Schema(name = "TimeSlot",
        description = "예약 단위가 되는 시간대. 코트가 운영하는 시간대와 정확히 일치해야 한다.",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record TimeSlotPayload(

        @Schema(description = "코트가 있는 지역 기준 시작 시각",
                type = "string",
                pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$",
                example = "06:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String startTime,

        @Schema(description = "코트가 있는 지역 기준 종료 시각",
                type = "string",
                pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$",
                example = "08:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String endTime) {
}
