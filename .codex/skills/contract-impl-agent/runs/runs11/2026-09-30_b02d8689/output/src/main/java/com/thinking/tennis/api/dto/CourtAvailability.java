package com.thinking.tennis.api.dto;

import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 계약의 {@code CourtAvailability} 다. 코트 하나의 하루치 예약 상태다. */
@Schema(extensions = @Extension(properties = @ExtensionProperty(
        name = "x-requirement", value = "[\"FR-7\", \"FR-3\", \"EF-1\"]", parseValue = true)))
public record CourtAvailability(

        @Schema(description = "서비스가 지원하는 코트의 식별자",
                pattern = Patterns.COURT_ID,
                minLength = 1,
                maxLength = Patterns.COURT_ID_MAX_LENGTH,
                example = "seoul-yangjae-1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String courtId,

        @Schema(description = "사람이 읽는 코트 이름",
                minLength = 1,
                example = "양재 시민의 숲 테니스장 1번",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String courtName,

        @Schema(example = "2026-10-04", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate date,

        @Schema(description = "이 결과를 외부 예약처에서 가져오는 데 성공한 시각",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant confirmedAt,

        @Schema(description = """
                true이면 가장 최근 확인이 실패해 그 전에 성공한 결과를 돌려준 것이다.
                이 값은 요청 처리의 실패를 뜻하지 않고 돌려준 데이터가 얼마나 최신인지를 뜻한다.""",
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean stale,

        @Schema(description = "오래된 결과를 돌려준 이유. `stale`이 false이면 null이다.",
                nullable = true,
                requiredMode = Schema.RequiredMode.REQUIRED)
        UpstreamFailureReason staleReason,

        @Schema(description = "외부 예약 사이트의 이 코트 예약 화면",
                format = "uri",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String reservationUrl,

        @Schema(description = """
                그 날짜에 이 코트가 운영하는 시간대 전부다. 여기에 없는 시간대로 알림을 신청하면 `SLOT_NOT_SUPPORTED`로 거절된다.
                예약처가 아직 그 날짜를 열지 않았으면 빈 배열이다.""",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<AvailabilitySlot> slots) {
}
