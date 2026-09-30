package com.thinking.tennis.api.dto;

import com.thinking.tennis.app.AlertView;
import com.thinking.tennis.domain.Alert;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 계약의 {@code Alert} 다.
 *
 * <p>값이 널일 수 있는 필드도 계약이 필수로 두었으므로 키는 언제나 응답에 실린다. 널을 지우면 키가
 * 사라지고, 사라진 키는 계약이 약속한 필드가 없는 것이 된다.
 */
@Schema(name = "Alert",
        description = """
                알림 신청 하나다.

                `status`와 `delivery`는 함께 읽는다. `delivery.status`가 `SENT`인 신청은 `status`가 `NOTIFIED`이고,
                `NOTIFIED`인 신청의 `delivery`는 언제나 `SENT`다. 감시 중인 신청의 `delivery`는 null이거나 `PENDING`이다.
                `FAILED`는 재시도를 모두 쓴 것이라, `status`가 감시 중이어도 이 신청에는 다시 알림이 나가지 않는다.
                """,
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
        extensions = @Extension(properties = @ExtensionProperty(
                name = "x-requirement", value = "[\"FR-2\",\"FR-5\"]", parseValue = true)))
public record AlertResponse(

        @Schema(description = "알림 신청의 식별자. 서버가 신청을 만들 때 발급한다.",
                type = "string",
                format = "uuid",
                example = "6f5b2f7e-9c1a-4f0e-9a4c-2b7d5e1a8c30",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String alertId,

        @Schema(description = "서비스가 지원하는 코트의 식별자",
                type = "string",
                minLength = 1,
                maxLength = 64,
                pattern = "^[a-z0-9][a-z0-9-]*$",
                example = "seoul-yangjae-1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String courtId,

        @Schema(description = "사람이 읽는 코트 이름",
                type = "string",
                minLength = 1,
                example = "양재 시민의 숲 테니스장 1번",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String courtName,

        @Schema(description = "외부 예약 사이트의 이 코트 예약 화면. 알림에 담기는 링크와 같다.",
                type = "string",
                format = "uri",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String reservationUrl,

        @Schema(type = "string",
                format = "date",
                example = "2026-10-04",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String date,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        TimeSlotPayload slot,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        AlertStatusValue status,

        @Schema(type = "string",
                format = "date-time",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String createdAt,

        @Schema(description = """
                이 신청이 만료되는 시각이다. 이용 시작 시각이며, 더 이른 예약 마감 시각이 확인되면 그 시각으로 앞당겨진다.
                신청한 뒤에도 바뀔 수 있으므로 클라이언트는 이 값을 캐시하지 말고 조회할 때마다 다시 읽는다.
                만료로 옮기는 일은 서버 작업이 하므로 이 시각이 지났는데도 `status`가 잠시 `WATCHING`으로 보일 수 있고,
                그 사이에는 이 시각을 기준으로 만료를 표시한다.
                """,
                type = "string",
                format = "date-time",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String expiresAt,

        @Schema(description = "이 신청의 코트·날짜를 마지막으로 확인하는 데 성공한 시각. 아직 한 번도 확인하지 못했으면 null이다.",
                type = "string",
                format = "date-time",
                nullable = true,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String lastCheckedAt,

        @Schema(description = """
                확인이 밀리고 있는지 여부다. FR-2가 정한 대로 마지막 성공 확인이 확인 간격의 세 배를 넘기면 true가 된다.
                한 번도 확인에 성공하지 못한 신청은 `createdAt`을 기준으로 같은 규칙을 적용한다.
                """,
                type = "boolean",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean checkDelayed,

        @Schema(description = "알림 발송 결과. 발송 대기에 오른 적이 없거나 그 대기가 취소됐으면 null이다.",
                nullable = true,
                requiredMode = Schema.RequiredMode.REQUIRED)
        AlertDeliveryResponse delivery) {

    public static AlertResponse of(AlertView view) {
        Alert alert = view.alert();
        return new AlertResponse(
                alert.alertId(),
                alert.courtId(),
                view.courtName(),
                view.reservationUrl(),
                alert.date().toString(),
                Payloads.slot(alert.slot()),
                AlertStatusValue.of(alert.status()),
                instant(alert.createdAt()),
                instant(alert.expiresAt()),
                instant(view.lastCheckedAt()),
                view.checkDelayed(),
                AlertDeliveryResponse.of(alert.delivery()));
    }

    private static String instant(Instant value) {
        return value == null ? null : value.toString();
    }
}
