package com.thinking.tennis.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thinking.tennis.app.AvailabilityView;
import com.thinking.tennis.domain.CourtDayAvailability;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * 계약의 {@code CourtAvailability} 다.
 */
@Schema(name = "CourtAvailability",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
        extensions = @Extension(properties = @ExtensionProperty(
                name = "x-requirement", value = "[\"FR-7\",\"FR-3\",\"EF-1\"]", parseValue = true)))
public record CourtAvailabilityResponse(

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

        @Schema(type = "string",
                format = "date",
                example = "2026-10-04",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String date,

        @Schema(description = "이 결과를 외부 예약처에서 가져오는 데 성공한 시각",
                type = "string",
                format = "date-time",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String confirmedAt,

        @Schema(description = """
                true이면 가장 최근 확인이 실패해 그 전에 성공한 결과를 돌려준 것이다.
                이 값은 요청 처리의 실패를 뜻하지 않고 돌려준 데이터가 얼마나 최신인지를 뜻한다.
                """,
                type = "boolean",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean stale,

        @Schema(description = "오래된 결과를 돌려준 이유. `stale`이 false이면 null이다.",
                nullable = true,
                requiredMode = Schema.RequiredMode.REQUIRED)
        UpstreamFailureReasonValue staleReason,

        @Schema(description = "외부 예약 사이트의 이 코트 예약 화면",
                type = "string",
                format = "uri",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String reservationUrl,

        /*
         * 필수 여부를 @JsonProperty 로도 적는다. 배열 필드의 필수 선언을 @ArraySchema 안쪽에만 두면
         * 추출되는 스펙의 required 목록에 이 이름이 실리지 않아, 계약이 항상 담겠다고 한 필드가
         * 선택으로 나간다. 두 자리가 같은 말을 하므로 어느 쪽이 읽히든 결과는 같다.
         */
        @JsonProperty(required = true)
        @ArraySchema(arraySchema = @Schema(
                description = """
                        그 날짜에 이 코트가 운영하는 시간대 전부다. 비어 있지 않으면 여기에 없는 시간대로 알림을 신청할 때 `SLOT_NOT_SUPPORTED`로 거절된다.
                        예약처가 아직 그 날짜를 열지 않았으면 빈 배열이다. 그때는 운영 시간대를 아직 판정할 수 없다는 뜻이라 어떤 시간대로 신청해도 그 코드로 거절되지 않는다.
                        """,
                requiredMode = Schema.RequiredMode.REQUIRED))
        List<AvailabilitySlotResponse> slots) {

    /*
     * 코트 이름과 예약 화면 주소는 저장된 확인 결과가 아니라 읽은 결과가 지닌 신원에서 가져온다.
     * 확인 결과는 오래될 수 있고, 오래된 결과와 함께 링크까지 낡으면 신청 조회가 보여 주는 링크와 갈린다.
     */
    public static CourtAvailabilityResponse of(AvailabilityView view) {
        CourtDayAvailability availability = view.availability();
        List<AvailabilitySlotResponse> slots = new ArrayList<>(availability.slots().size());
        for (CourtDayAvailability.SlotStatus status : availability.slots()) {
            slots.add(AvailabilitySlotResponse.of(status));
        }
        return new CourtAvailabilityResponse(
                availability.courtId(),
                view.court().courtName(),
                availability.date().toString(),
                availability.confirmedAt().toString(),
                view.stale(),
                UpstreamFailureReasonValue.of(view.staleReason()),
                view.court().reservationUrl(),
                List.copyOf(slots));
    }
}
