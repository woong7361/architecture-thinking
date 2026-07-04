package com.thinking.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 일할계산(PRORATION) 순수 도메인 로직 단위테스트.
 *
 * 규칙:
 *  - 입력: (price, totalDays, elapsedDays), 내부에서 remaining = totalDays - elapsedDays
 *  - 일단가를 원 단위로 먼저 버림한 뒤 remaining 을 곱한다 (버림 우선)
 *  - 외부 의존 없음 → Mock 없이 진짜 실행한다.
 */
class ProrationTest {

    // 삼각측량으로 확보한 두 데이터 점을 파라미터 행으로 유지한다.
    // 메서드 구조(중복 보일러플레이트)만 하나로 합쳤을 뿐, 데이터 점은 줄이지 않는다 —
    // 점을 하나로 줄이면 함수가 다시 과소결정되어 하드코딩 뮤턴트가 되살아난다.
    @ParameterizedTest(name = "{0}원 {1}일 중 {2}일 사용 → 남은 기간 잔여금액 {3}원")
    @DisplayName("일할계산: 일단가(price/total 버림) × 남은 일수(total-elapsed) 가 잔여금액이다")
    @CsvSource({
        "30000, 30, 15, 15000", // 일단가 1000 × 남은 15일
        "30000, 30, 20, 10000"  // 일단가 1000 × 남은 10일 (삼각측량 두 번째 점)
    })
    void 남은_기간만큼_일할계산한_금액이_잔여금액이다(int price, int totalDays, int elapsedDays, int expected) {
        int remaining = Proration.calculate(price, totalDays, elapsedDays);

        assertThat(remaining).isEqualTo(expected);
    }
}
