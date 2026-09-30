package com.thinking.tennis.domain;

/**
 * 외부 예약처 확인이 실패한 이유다.
 *
 * <p>이름은 포트의 {@code CourtAvailabilityPort.UpstreamFailureReason} 과 계약의
 * {@code UpstreamFailureReason} 과 같게 둔다. 저장된 성공 결과가 없었다면 같은 이름의 에러 코드로 나갈
 * 실패라서, 중간에 다른 이름을 두면 옮겨 적는 자리만 늘고 얻는 것이 없다.
 */
public enum UpstreamFailureReason {

    /** 정해진 시간 안에 응답이 오지 않았다. */
    UPSTREAM_TIMEOUT,

    /** 예약처에 닿지 못했거나 예약처가 실패를 돌려줬다. */
    UPSTREAM_UNAVAILABLE,

    /** 응답은 왔지만 내용을 읽지 못했다. */
    UPSTREAM_RESPONSE_UNREADABLE
}
