package com.thinking.tennis.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 계약의 {@code Problem} 이다. RFC 9457 형식의 실패 응답이다.
 *
 * <p>본문은 이 레코드가 아니라 {@link #toBody()} 가 만든 표로 나간다. 계약은 {@code traceId} 를 5xx에만
 * 싣고 {@code retryAfterSeconds} 를 재시도 가능한 실패에만 싣는다고 했는데, 전역 직렬화 설정은 널을
 * 지우지 않는 쪽으로 고정돼 있어서 레코드를 그대로 내보내면 4xx 응답에도 {@code traceId} 키가 널로
 * 따라 나간다. 그 설정은 계약이 널을 허용하면서 필수로 둔 필드의 키를 지키는 장치이므로 덮어쓰지 않고,
 * 실을 것만 담은 표를 만들어 지킨다.
 */
@Schema(description = """
        RFC 9457 형식의 실패 응답이다. `code`가 원인을 가리키고 `retryable`이 클라이언트의 다음 행동을 정한다.
        `retryable`이 true이면 `retryAfterSeconds`와 `Retry-After` 헤더가 반드시 함께 온다.
        `traceId`는 5xx에만 싣는다. 4xx는 클라이언트가 요청을 고치면 풀려 서버 로그를 찾을 일이 없다.
        두 조건 모두 스키마로는 적을 수 없어 계약 검사 스크립트가 대신 판정한다.""")
public record Problem(

        @Schema(description = "이 실패 종류를 설명하는 문서의 주소",
                format = "uri",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String type,

        @Schema(description = "사람이 읽는 실패 이름",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Schema(description = "HTTP 상태 코드. 응답의 상태 코드와 같다.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Integer status,

        @Schema(description = "이번 실패에만 해당하는 설명")
        String detail,

        @Schema(description = "실패가 난 요청의 경로", format = "uri-reference")
        String instance,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        ErrorCode code,

        @Schema(description = """
                같은 요청을 그대로 다시 보내 결과가 달라질 수 있으면 true다.
                false이면 요청이나 조건을 바꾸지 않는 한 몇 번을 보내도 같은 실패가 난다.""",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean retryable,

        @Schema(description = "다시 시도하기까지 기다릴 초", minimum = "1")
        Integer retryAfterSeconds,

        @Schema(description = """
                서버 로그에서 이 요청을 찾는 식별자다. 사용자가 실패를 신고할 때 이 값으로 로그를 찾는다.
                5xx에만 오고 4xx에는 오지 않는다.""")
        String traceId) {

    /** 계약이 실으라고 한 키만 담은 본문이다. 키 순서는 계약이 적은 순서를 따른다. */
    public Map<String, Object> toBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type);
        body.put("title", title);
        body.put("status", status);
        if (detail != null) {
            body.put("detail", detail);
        }
        if (instance != null) {
            body.put("instance", instance);
        }
        body.put("code", code.name());
        body.put("retryable", retryable);
        if (retryAfterSeconds != null) {
            body.put("retryAfterSeconds", retryAfterSeconds);
        }
        if (traceId != null) {
            body.put("traceId", traceId);
        }
        return body;
    }
}
