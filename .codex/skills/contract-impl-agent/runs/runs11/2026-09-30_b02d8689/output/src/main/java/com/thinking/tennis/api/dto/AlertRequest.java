package com.thinking.tennis.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 계약의 {@code AlertRequest} 다. 알림 신청 조건이며 코트 하나, 날짜 하나, 시간대 하나다.
 *
 * <p>값 검사는 {@code RequestValidation} 이 한다. 스켈레톤의 의존에는 빈 검증 구현이 없어 선언만 붙여
 * 두면 아무것도 검사하지 않는 채로 통과한다. 검사하지 않고 통과하면 계약이 400으로 거절하라고 한 요청이
 * 저장까지 내려간다.
 */
@Schema(description = "알림 신청 조건. 코트 하나, 날짜 하나, 시간대 하나다.")
public record AlertRequest(

        @Schema(description = "서비스가 지원하는 코트의 식별자",
                pattern = Patterns.COURT_ID,
                minLength = 1,
                maxLength = Patterns.COURT_ID_MAX_LENGTH,
                example = "seoul-yangjae-1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String courtId,

        @Schema(description = "이용하려는 날짜. 코트가 있는 지역의 날짜다.",
                example = "2026-10-04",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate date,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        TimeSlot slot) {
}
