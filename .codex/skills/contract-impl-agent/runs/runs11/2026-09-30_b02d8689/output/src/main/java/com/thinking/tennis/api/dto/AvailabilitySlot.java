package com.thinking.tennis.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 계약의 {@code AvailabilitySlot} 이다. */
public record AvailabilitySlot(

        @Schema(pattern = Patterns.TIME, example = "06:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String startTime,

        @Schema(pattern = Patterns.TIME, example = "08:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String endTime,

        @Schema(description = """
                이 시간대가 마지막 확인 시점에 비어 있었는지 여부다. 확인에 실패한 경우의 값이 아니다.
                확인 실패는 `stale`로 드러나며, `stale`이 true이면 이 값은 `confirmedAt` 시점의 사실이다.""",
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean available) {
}
