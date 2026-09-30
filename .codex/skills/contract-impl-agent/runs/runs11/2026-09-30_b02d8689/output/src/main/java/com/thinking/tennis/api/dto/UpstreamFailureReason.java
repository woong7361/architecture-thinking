package com.thinking.tennis.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 계약의 {@code UpstreamFailureReason} 이다. 포트의 같은 이름 열거형과 값이 같다. 저장된 성공 결과가
 * 없었다면 같은 이름의 에러 코드로 나갔을 실패라서 중간에 다른 이름을 두지 않는다.
 */
@Schema(description = """
        외부 예약처 확인이 실패한 이유. 저장된 성공 결과가 없었다면 같은 이름의 에러 코드로 나갔을 실패다.

        - `UPSTREAM_TIMEOUT`: 정해진 시간 안에 응답이 오지 않았다.
        - `UPSTREAM_UNAVAILABLE`: 예약처에 닿지 못했거나 예약처가 실패를 돌려줬다.
        - `UPSTREAM_RESPONSE_UNREADABLE`: 응답은 왔지만 내용을 읽지 못했다.""")
public enum UpstreamFailureReason {

    UPSTREAM_TIMEOUT,
    UPSTREAM_UNAVAILABLE,
    UPSTREAM_RESPONSE_UNREADABLE;

    public static UpstreamFailureReason of(
            com.thinking.tennis.port.CourtAvailabilityPort.UpstreamFailureReason reason) {
        return reason == null ? null : valueOf(reason.name());
    }
}
