package com.thinking.tennis.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 계약의 {@code AlertRequest} 다.
 *
 * <p>계약이 정하지 않은 필드를 더 받아도 무시한다. 요청을 계약보다 넓게 받는 것은 허용이고 좁게 받는
 * 것이 위반이다.
 */
@Schema(name = "AlertRequest",
        description = "알림 신청 조건. 코트 하나, 날짜 하나, 시간대 하나다.",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CreateAlertRequest(

        @Schema(description = "서비스가 지원하는 코트의 식별자",
                type = "string",
                minLength = 1,
                maxLength = 64,
                pattern = "^[a-z0-9][a-z0-9-]*$",
                example = "seoul-yangjae-1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String courtId,

        @Schema(description = "이용하려는 날짜. 코트가 있는 지역의 날짜다.",
                type = "string",
                format = "date",
                example = "2026-10-04",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String date,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        TimeSlotPayload slot) {
}
