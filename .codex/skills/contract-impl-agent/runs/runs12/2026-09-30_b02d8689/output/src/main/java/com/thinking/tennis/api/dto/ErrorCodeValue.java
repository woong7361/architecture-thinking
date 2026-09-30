package com.thinking.tennis.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 계약의 {@code ErrorCode} 다. 클라이언트의 대응이 갈리는 자리마다 코드 하나를 둔다.
 */
@Schema(name = "ErrorCode",
        description = """
                실패의 원인을 가리키는 코드. 클라이언트의 대응이 갈리는 자리마다 코드 하나를 둔다.
                대응이 같은 두 상황에는 코드를 따로 두지 않는다. 구별해도 클라이언트가 분기할 일이 없어 무시된다.

                - `VALIDATION_FAILED`: 요청의 형식이나 값이 스키마를 만족하지 않는다. 필수인 `Idempotency-Key` 헤더가 없거나 그 값이 UUID가 아닌 경우도 여기에 든다.
                - `UNAUTHENTICATED`: 토큰이 없거나 유효하지 않아 사용자를 식별할 수 없다.
                - `COURT_NOT_SUPPORTED`: 서비스가 지원하지 않는 코트다. 경로에서 받았으면 404, 본문에서 받았으면 422로 나간다.
                - `SLOT_NOT_SUPPORTED`: 코트는 지원하지만 그 코트가 그 날짜에 운영하지 않는 시간대다.
                - `ALERT_WINDOW_CLOSED`: 만료 시각이 이미 지나 감시할 수 없는 조건이다.
                - `ALERT_NOT_FOUND`: 그런 신청이 없거나 요청한 사용자의 것이 아니다.
                - `IDEMPOTENCY_KEY_REUSED`: 같은 키가 다른 내용의 요청에 이미 쓰였다.
                - `UPSTREAM_TIMEOUT`: 외부 예약처가 정해진 시간 안에 응답하지 않았다.
                - `UPSTREAM_UNAVAILABLE`: 외부 예약처에 닿지 못했거나 예약처가 실패를 돌려줬다.
                - `UPSTREAM_RESPONSE_UNREADABLE`: 외부 예약처의 응답을 읽지 못했다.
                - `STORAGE_TIMEOUT`: 저장소가 정해진 시간 안에 응답하지 않았다.
                - `CONCURRENT_UPDATE_CONFLICT`: 같은 자원을 동시에 고치려다 경합에 밀렸다. 같은 멱등 키의 앞선 요청이 아직 처리 중이라 물러난 경우도 여기에 든다.
                - `INTERNAL_ERROR`: 위 어디에도 해당하지 않는 서버 내부 실패다. 상태를 바꾸는 오퍼레이션은 멱등 키를 요구하므로 같은 요청을 그대로 다시 보내도 안전하다.
                """)
public enum ErrorCodeValue {

    VALIDATION_FAILED,
    UNAUTHENTICATED,
    COURT_NOT_SUPPORTED,
    SLOT_NOT_SUPPORTED,
    ALERT_WINDOW_CLOSED,
    ALERT_NOT_FOUND,
    IDEMPOTENCY_KEY_REUSED,
    UPSTREAM_TIMEOUT,
    UPSTREAM_UNAVAILABLE,
    UPSTREAM_RESPONSE_UNREADABLE,
    STORAGE_TIMEOUT,
    CONCURRENT_UPDATE_CONFLICT,
    INTERNAL_ERROR
}
