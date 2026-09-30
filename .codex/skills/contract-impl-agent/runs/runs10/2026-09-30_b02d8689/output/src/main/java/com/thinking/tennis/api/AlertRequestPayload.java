package com.thinking.tennis.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AlertRequest", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record AlertRequestPayload(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1,
                maxLength = 64,
                pattern = "^[a-z0-9][a-z0-9-]*$")
        String courtId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
        String date,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        TimeSlotPayload slot) {

    @Schema(name = "TimeSlot", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record TimeSlotPayload(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
            String startTime,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                    pattern = "^([01][0-9]|2[0-3]):[0-5][0-9]$")
            String endTime) {
    }
}
