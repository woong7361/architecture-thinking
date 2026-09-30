package com.thinking.tennis.api.dto;

import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** 계약의 {@code Alert} 다. 알림 신청 하나다. */
@Schema(description = """
        알림 신청 하나다.

        `status`와 `delivery`는 함께 읽는다. `delivery.status`가 `SENT`인 신청은 `status`가 `NOTIFIED`이고,
        `NOTIFIED`인 신청의 `delivery`는 언제나 `SENT`다. 감시 중인 신청의 `delivery`는 null이거나 `PENDING`이다.
        `FAILED`는 재시도를 모두 쓴 것이라, `status`가 감시 중이어도 이 신청에는 다시 알림이 나가지 않는다.""",
        extensions = @Extension(properties = @ExtensionProperty(
                name = "x-requirement", value = "[\"FR-2\", \"FR-5\"]", parseValue = true)))
public record Alert(

        @Schema(description = "알림 신청의 식별자. 서버가 신청을 만들 때 발급한다.",
                format = "uuid",
                example = "6f5b2f7e-9c1a-4f0e-9a4c-2b7d5e1a8c30",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID alertId,

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

        @Schema(description = "외부 예약 사이트의 이 코트 예약 화면. 알림에 담기는 링크와 같다.",
                format = "uri",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String reservationUrl,

        @Schema(example = "2026-10-04", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate date,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        TimeSlot slot,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        AlertStatus status,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = """
                이 신청이 만료되는 시각이다. 이용 시작 시각이며, 더 이른 예약 마감 시각이 확인되면 그 시각으로 앞당겨진다.
                신청한 뒤에도 바뀔 수 있으므로 클라이언트는 이 값을 캐시하지 말고 조회할 때마다 다시 읽는다.
                만료로 옮기는 일은 서버 작업이 하므로 이 시각이 지났는데도 `status`가 잠시 `WATCHING`으로 보일 수 있고,
                그 사이에는 이 시각을 기준으로 만료를 표시한다.""",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant expiresAt,

        @Schema(description = """
                이 신청의 코트·날짜를 마지막으로 확인하는 데 성공한 시각. 아직 한 번도 확인하지 못했으면 null이다.""",
                nullable = true,
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant lastCheckedAt,

        @Schema(description = """
                확인이 밀리고 있는지 여부다. FR-2가 정한 대로 마지막 성공 확인이 확인 간격의 세 배를 넘기면 true가 된다.
                한 번도 확인에 성공하지 못한 신청은 `createdAt`을 기준으로 같은 규칙을 적용한다.""",
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean checkDelayed,

        @Schema(description = "알림 발송 결과. 발송 대기에 오른 적이 없거나 그 대기가 취소됐으면 null이다.",
                nullable = true,
                requiredMode = Schema.RequiredMode.REQUIRED)
        AlertDelivery delivery) {
}
