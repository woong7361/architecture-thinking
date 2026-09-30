package com.thinking.tennis.api.dto;

import com.thinking.tennis.domain.AlertDelivery;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 계약의 {@code AlertDelivery} 다.
 */
@Schema(name = "AlertDelivery",
        description = """
                이 신청에 대한 알림 발송 결과다. 신청 하나에 알림은 한 건만 보낸다.
                실제로 보내기 전까지는 취소될 수 있어서, `PENDING`이던 대기가 사라지고 `delivery`가 다시 null이 되는 경우가 있다.
                최신 확인 결과에 빈자리가 없어 보내지 않기로 했을 때와 신청이 해제되거나 만료됐을 때다.
                """,
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
        extensions = @Extension(properties = @ExtensionProperty(
                name = "x-requirement", value = "[\"FR-4\",\"EF-3\",\"EF-5\",\"V-2\",\"V-7\"]", parseValue = true)))
public record AlertDeliveryResponse(

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        DeliveryStatusValue status,

        @Schema(description = """
                발송을 시도한 횟수다. 아직 보내지 않았으면 0이고, 상한은 최초 발송 한 번에 EF-3이 정한 재시도 세 번을 더한 값이다.
                """,
                type = "integer",
                minimum = "0",
                maximum = "4",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Integer attemptCount,

        @Schema(description = "마지막으로 시도한 시각. 아직 보내지 않았으면 null이다.",
                type = "string",
                format = "date-time",
                nullable = true,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String lastAttemptAt,

        @Schema(description = """
                실패한 경우 사람이 읽을 사유다. 실패하지 않았으면 null이다.
                값의 집합을 계약이 정하지 않으므로 클라이언트는 이 값으로 분기하지 말고 `status`를 쓴다.
                """,
                type = "string",
                nullable = true,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String failureReason) {

    public static AlertDeliveryResponse of(AlertDelivery delivery) {
        if (delivery == null) {
            return null;
        }
        return new AlertDeliveryResponse(
                DeliveryStatusValue.of(delivery.status()),
                delivery.attemptCount(),
                instant(delivery.lastAttemptAt()),
                delivery.failureReason());
    }

    private static String instant(Instant value) {
        return value == null ? null : value.toString();
    }
}
