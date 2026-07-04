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

    @ParameterizedTest(name = "{0}원 {1}일 중 {2}일 경과 → 무료환불 전액 {3}원")
    @DisplayName("무료환불 정책: elapsed<=7 이면 일할계산과 무관하게 전액(price)이 환불된다")
    @CsvSource({
        "30000, 30, 7, 30000" // 정책 경계: elapsed=7(포함) → 전액. 일할계산이면 1000*23=23000이라 이 값이 정책을 강제한다.
    })
    void 경과일이_칠일_이하면_일할계산과_무관하게_전액_환불된다(int price, int totalDays, int elapsedDays, int expected) {
        int remaining = RefundCalculator.calculate(price, totalDays, elapsedDays);

        assertThat(remaining).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}원 {1}일 중 {2}일 경과 → 일할계산 전환 {3}원")
    @DisplayName("무료환불 정책 경계 전환: elapsed=8(>7)부터는 전액이 아니라 일할계산 금액이 환불된다")
    @CsvSource({
        "30000, 30, 8, 22000" // 9번(elapsed=7→전액30000)과 straddle pair. 일단가1000×남은22일=22000
    })
    void 경과일이_칠일_초과면_일할계산금액이_환불된다(int price, int totalDays, int elapsedDays, int expected) {
        int remaining = RefundCalculator.calculate(price, totalDays, elapsedDays);

        assertThat(remaining).isEqualTo(expected);
    }
}
