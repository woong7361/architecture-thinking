package com.thinking.tennis.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 계약의 {@code Problem} 이다. RFC 9457 형식의 실패 응답이다.
 *
 * <p>선택 필드가 널을 허용하지 않으므로 값이 없으면 키를 싣지 않는다. 필수 필드는 언제나 값이 있어
 * 키가 빠지지 않는다.
 */
@Schema(name = "Problem",
        description = """
                RFC 9457 형식의 실패 응답이다. `code`가 원인을 가리키고 `retryable`이 클라이언트의 다음 행동을 정한다.
                `retryable`이 true이면 `retryAfterSeconds`와 `Retry-After` 헤더가 반드시 함께 온다.
                `traceId`는 5xx에만 싣는다. 4xx는 클라이언트가 요청을 고치면 풀려 서버 로그를 찾을 일이 없다.
                두 조건 모두 스키마로는 적을 수 없어 계약 검사 스크립트가 대신 판정한다.
                """,
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record ProblemResponse(

        @Schema(description = "이 실패 종류를 설명하는 문서의 주소",
                type = "string",
                format = "uri",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String type,

        @Schema(description = "사람이 읽는 실패 이름",
                type = "string",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Schema(description = "HTTP 상태 코드. 응답의 상태 코드와 같다.",
                type = "integer",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Integer status,

        @Schema(description = "이번 실패에만 해당하는 설명",
                type = "string",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String detail,

        @Schema(description = "실패가 난 요청의 경로",
                type = "string",
                format = "uri-reference",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String instance,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        ErrorCodeValue code,

        @Schema(description = """
                같은 요청을 그대로 다시 보내 결과가 달라질 수 있으면 true다.
                false이면 요청이나 조건을 바꾸지 않는 한 몇 번을 보내도 같은 실패가 난다.
                """,
                type = "boolean",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean retryable,

        @Schema(description = "다시 시도하기까지 기다릴 초",
                type = "integer",
                minimum = "1",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Integer retryAfterSeconds,

        @Schema(description = """
                서버 로그에서 이 요청을 찾는 식별자다. 사용자가 실패를 신고할 때 이 값으로 로그를 찾는다.
                5xx에만 오고 4xx에는 오지 않는다.
                """,
                type = "string",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String traceId) {
}
