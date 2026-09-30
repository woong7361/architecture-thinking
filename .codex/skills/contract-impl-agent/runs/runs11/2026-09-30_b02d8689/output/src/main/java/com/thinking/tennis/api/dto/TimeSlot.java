package com.thinking.tennis.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 계약의 {@code TimeSlot} 이다. 요청과 응답이 같은 모양을 쓴다.
 *
 * <p>시각을 {@code LocalTime} 이 아니라 문자열로 든 것은 계약이 이 값을 형식 없는 문자열에 정규식을
 * 붙여 적었기 때문이다. 시각 타입으로 두면 추출한 스펙이 계약과 다른 형식을 말하고, 초가 붙은 값이
 * 나갈 여지도 생긴다.
 */
@Schema(description = "예약 단위가 되는 시간대. 코트가 운영하는 시간대와 정확히 일치해야 한다.")
public record TimeSlot(

        @Schema(description = "코트가 있는 지역 기준 시작 시각",
                pattern = Patterns.TIME,
                example = "06:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String startTime,

        @Schema(description = "코트가 있는 지역 기준 종료 시각",
                pattern = Patterns.TIME,
                example = "08:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String endTime) {
}
