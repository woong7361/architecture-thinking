package com.thinking.payment;

/**
 * 환불 계산 진입점 — 무료환불 기간 판정 + 일할계산({@link Proration}) 조합.
 *
 * elapsedDays <= FREE_REFUND_DAY_LIMIT 이면 사용량과 무관하게 전액 환불,
 * 아니면 순수 계산({@code Proration.calculate})에 위임한다.
 * 이 클래스가 바뀌는 이유는 "정책이 바뀔 때"뿐이고, Proration이 바뀌는 이유는
 * "계산 규칙이 바뀔 때"뿐이도록 축을 분리했다(SRP).
 */
public final class RefundCalculator {

    private static final int FREE_REFUND_DAY_LIMIT = 7;

    private RefundCalculator() {
    }

    public static int calculate(int price, int totalDays, int elapsedDays) {
        validateInputs(price, totalDays, elapsedDays);
        if (elapsedDays <= FREE_REFUND_DAY_LIMIT) {
            return price;
        }
        return Proration.calculate(price, totalDays, elapsedDays);
    }

    // 입력검증은 정책 판정보다 앞단이다(도메인 규칙: "검증이 정책보다 먼저").
    // private 메서드 그룹핑 = 가독성 정리이지 책임 분리가 아니다. 검증 불변식이
    // 정책과 독립적으로 변할 압력이 생기면 그때 별도 Validator 클래스로 승격을 재검토한다.
    private static void validateInputs(int price, int totalDays, int elapsedDays) {
        if (price < 0) {
            throw new IllegalArgumentException("금액은 음수일 수 없습니다: " + price);
        }
        if (elapsedDays < 0 || elapsedDays > totalDays) {
            throw new IllegalArgumentException(
                "경과일수는 0 이상 총일수 이하여야 합니다: elapsed=" + elapsedDays + ", total=" + totalDays);
        }
    }
}
