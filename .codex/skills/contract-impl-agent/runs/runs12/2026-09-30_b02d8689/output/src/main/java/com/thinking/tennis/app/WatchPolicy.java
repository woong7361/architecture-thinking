package com.thinking.tennis.app;

import java.time.Duration;
import java.time.ZoneId;

/**
 * 계약과 요구사항 명세가 정한 수치를 한자리에 모은다.
 *
 * <p>여기 있는 값은 이 구현이 고른 것이 아니라 명세에서 온 것이다. 코드 여기저기에 흩어 두면 어느 값이
 * 명세에서 왔고 어느 값이 구현이 고른 것인지 구별되지 않는다.
 */
public final class WatchPolicy {

    /** 확인 간격이다. NFR-3이 코트·날짜마다 20초로 정했다. */
    public static final Duration CHECK_INTERVAL = Duration.ofSeconds(20);

    /** 확인 지연으로 보는 배수다. FR-2가 마지막 성공 확인이 확인 간격의 세 배를 넘긴 상태로 정했다. */
    public static final int CHECK_DELAY_INTERVALS = 3;

    /** 확인 지연 문턱이다. */
    public static final Duration CHECK_DELAY_THRESHOLD = CHECK_INTERVAL.multipliedBy(CHECK_DELAY_INTERVALS);

    /** 코트가 있는 지역의 시간대다. 계약이 현재 지원 지역을 Asia/Seoul로 적었다. */
    public static final ZoneId COURT_ZONE = ZoneId.of("Asia/Seoul");

    /** 멱등 키와 그 응답을 보관하는 기간이다. 계약이 24시간으로 정했다. */
    public static final Duration IDEMPOTENCY_RETENTION = Duration.ofHours(24);

    private WatchPolicy() {
    }
}
