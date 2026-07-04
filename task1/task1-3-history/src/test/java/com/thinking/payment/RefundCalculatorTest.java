package com.thinking.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 환불 계산(무료환불 기간 판정 + 일할계산 조합) 단위테스트.
 *
 * 순수 로직(외부 의존 없음) → Mock 없이 진짜 실행한다.
 * Proration(순수 계산)과 분리된 seam: 여기는 "얼마나 남았나"가 아니라
 * "정책상 전액인가, 일할계산인가"를 먼저 판정한 뒤 최종 환불액을 낸다.
 */
class RefundCalculatorTest {

    // straddle pair: elapsed=7과 8 두 행을 한 테이블에 나란히 둬 정책 경계(≤7 전액 / >7 일할계산)를
    // 한눈에 대조할 수 있게 한다. 구조(호출+단언)가 같은 두 케이스를 병합했다(사이클2와 동일한 판단).
    @ParameterizedTest(name = "{0}원 {1}일 중 {2}일 경과 → 환불 {3}원")
    @DisplayName("무료환불 정책 경계: elapsed<=7 이면 전액(price), elapsed>7 이면 일할계산 금액이 환불된다")
    @CsvSource({
        "30000, 30, 7, 30000", // 정책 경계: elapsed=7(포함) → 전액. 일할계산이면 1000*23=23000이라 이 값이 정책을 강제한다.
        "30000, 30, 8, 22000"  // straddle pair: elapsed=8 → 일할계산 위임, 일단가1000×남은22일=22000
    })
    void 무료환불_경계를_기준으로_전액_또는_일할계산_금액이_환불된다(int price, int totalDays, int elapsedDays, int expected) {
        int remaining = RefundCalculator.calculate(price, totalDays, elapsedDays);

        assertThat(remaining).isEqualTo(expected);
    }
}
