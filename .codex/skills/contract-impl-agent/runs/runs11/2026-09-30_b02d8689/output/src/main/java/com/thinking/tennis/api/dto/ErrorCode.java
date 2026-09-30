package com.thinking.tennis.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 계약의 {@code ErrorCode} 다. 실패의 원인을 가리키는 코드이며 클라이언트의 대응이 갈리는 자리마다
 * 코드 하나를 둔다.
 *
 * <p>코드마다 계약이 예시로 적은 {@code type} 과 {@code title} 을 함께 들고 있다. 같은 코드로 나가는
 * 실패가 자리마다 다른 문서 주소와 다른 이름을 말하면, 클라이언트가 코드로 분기하고 사람이 이름으로
 * 읽는 두 경로가 어긋난다.
 */
@Schema(description = """
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
        - `INTERNAL_ERROR`: 위 어디에도 해당하지 않는 서버 내부 실패다. 상태를 바꾸는 오퍼레이션은 멱등 키를 요구하므로 같은 요청을 그대로 다시 보내도 안전하다.""")
public enum ErrorCode {

    VALIDATION_FAILED("validation-failed", "요청 값이 올바르지 않습니다"),
    UNAUTHENTICATED("unauthenticated", "인증이 필요합니다"),
    COURT_NOT_SUPPORTED("court-not-supported", "지원하지 않는 코트입니다"),
    SLOT_NOT_SUPPORTED("slot-not-supported", "지원하지 않는 시간대입니다"),
    ALERT_WINDOW_CLOSED("alert-window-closed", "이미 지난 시간대입니다"),
    ALERT_NOT_FOUND("alert-not-found", "신청을 찾을 수 없습니다"),
    IDEMPOTENCY_KEY_REUSED("idempotency-key-reused", "이미 다른 요청에 쓰인 멱등 키입니다"),
    UPSTREAM_TIMEOUT("upstream-timeout", "예약 상태를 확인하지 못했습니다"),
    UPSTREAM_UNAVAILABLE("upstream-unavailable", "예약 상태를 확인하지 못했습니다"),
    UPSTREAM_RESPONSE_UNREADABLE("upstream-response-unreadable", "예약 상태를 확인하지 못했습니다"),
    STORAGE_TIMEOUT("storage-timeout", "일시적으로 처리할 수 없습니다"),
    CONCURRENT_UPDATE_CONFLICT("concurrent-update-conflict", "일시적으로 처리할 수 없습니다"),
    INTERNAL_ERROR("internal-error", "요청을 처리하지 못했습니다");

    private static final String PROBLEM_BASE = "https://api.tennis-alert.example.com/problems/";

    private final String slug;
    private final String title;

    ErrorCode(String slug, String title) {
        this.slug = slug;
        this.title = title;
    }

    /** 이 실패 종류를 설명하는 문서의 주소다. */
    public String problemType() {
        return PROBLEM_BASE + slug;
    }

    /** 사람이 읽는 실패 이름이다. */
    public String title() {
        return title;
    }
}
