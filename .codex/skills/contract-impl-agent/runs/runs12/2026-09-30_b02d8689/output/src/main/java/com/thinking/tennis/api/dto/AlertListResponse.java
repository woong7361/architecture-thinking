package com.thinking.tennis.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thinking.tennis.app.AlertView;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * 계약의 {@code AlertList} 다.
 *
 * <p>페이지를 나누지 않는다. 감시 중인 신청은 1인당 두세 건이라고 NFR-1이 보았고, 끝난 신청은 보관
 * 기간이 정해지기 전까지 상한이 없다.
 */
@Schema(name = "AlertList", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record AlertListResponse(

        /*
         * 필수 여부를 @JsonProperty 로도 적는다. 배열 필드의 필수 선언을 @ArraySchema 안쪽에만 두면
         * 추출되는 스펙의 required 목록에 이 이름이 실리지 않아, 계약이 항상 담겠다고 한 필드가
         * 선택으로 나간다. 두 자리가 같은 말을 하므로 어느 쪽이 읽히든 결과는 같다.
         */
        @JsonProperty(required = true)
        @ArraySchema(arraySchema = @Schema(
                description = "본인의 신청 전부. 최근에 신청한 것이 앞에 온다.",
                requiredMode = Schema.RequiredMode.REQUIRED))
        List<AlertResponse> items) {

    public static AlertListResponse of(List<AlertView> views) {
        List<AlertResponse> items = new ArrayList<>(views.size());
        for (AlertView view : views) {
            items.add(AlertResponse.of(view));
        }
        return new AlertListResponse(List.copyOf(items));
    }
}
