package com.thinking.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 일할계산(PRORATION) 순수 도메인 로직 단위테스트.
 *
 * 규칙:
 *  - 입력: (price, totalDays, elapsedDays), 내부에서 remaining = totalDays - elapsedDays
 *  - 일단가를 원 단위로 먼저 버림한 뒤 remaining 을 곱한다 (버림 우선)
 *  - 외부 의존 없음 → Mock 없이 진짜 실행한다.
 */
class ProrationTest {

    @Test
    @DisplayName("30000원 30일 상품을 15일 쓰면 남은 15일치 15000원이 잔여금액이다")
    void 절반을_사용하면_남은_기간만큼_잔여금액이_나온다() {
        // 일단가 30000/30 = 1000, 남은 15일 → 1000 * 15 = 15000
        int remaining = Proration.calculate(30000, 30, 15);

        assertThat(remaining).isEqualTo(15000);
    }

    @Test
    @DisplayName("30000원 30일 상품을 20일 쓰면 남은 10일치 10000원이 잔여금액이다")
    void 다른_비율에서도_남은_기간만큼_잔여금액이_나온다() {
        // 일단가 30000/30 = 1000, 남은 10일 → 1000 * 10 = 10000
        // (하드코딩 return 15000 을 깨뜨려 일반식을 강제하는 삼각측량 케이스)
        int remaining = Proration.calculate(30000, 30, 20);

        assertThat(remaining).isEqualTo(10000);
    }
}
