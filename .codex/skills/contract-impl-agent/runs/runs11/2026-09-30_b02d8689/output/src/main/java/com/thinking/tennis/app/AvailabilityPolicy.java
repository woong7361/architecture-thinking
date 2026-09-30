package com.thinking.tennis.app;

import java.time.Duration;
import java.time.ZoneId;

/**
 * 확인 간격과 지역 시간대처럼 계약과 요구사항이 정한 값이다.
 *
 * <p>계약은 확인 간격의 값을 직접 적지 않고 요구사항 NFR-3을 가리키므로 그 문서에 적힌 20초를 쓴다.
 * 확인 실패로 나가는 응답의 {@code retryAfterSeconds} 도 같은 값을 따르라고 계약이 적었다.
 */
public final class AvailabilityPolicy {

    /** NFR-3: 코트·날짜마다 20초. 이 간격 안에 들어온 요청은 예약처를 다시 조회하지 않는다. */
    public static final Duration CHECK_INTERVAL = Duration.ofSeconds(20);

    /** FR-2: 마지막 성공 확인이 확인 간격의 세 배를 넘기면 확인이 밀린 것으로 본다. */
    public static final Duration CHECK_DELAY_THRESHOLD = CHECK_INTERVAL.multipliedBy(3);

    /** 계약의 시간 표현 절이 정한 현재 지원 지역의 시간대. 날짜와 시각을 시점으로 바꿀 때 쓴다. */
    public static final ZoneId COURT_ZONE = ZoneId.of("Asia/Seoul");

    private AvailabilityPolicy() {
    }
}
