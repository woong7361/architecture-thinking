package com.thinking.payment;

/**
 * 환불 정책 — 무료환불 기간 판정 + 일할계산({@link Proration}) 조합.
 *
 * elapsedDays <= FREE_REFUND_DAY_LIMIT 이면 사용량과 무관하게 전액 환불,
 * 아니면 순수 계산({@code Proration.calculate})에 위임한다.
 * 이 클래스가 바뀌는 이유는 "정책이 바뀔 때"뿐이고, Proration이 바뀌는 이유는
 * "계산 규칙이 바뀔 때"뿐이도록 축을 분리했다(SRP).
 */
public final class RefundPolicy {

    private static final int FREE_REFUND_DAY_LIMIT = 7;

    private RefundPolicy() {
    }

    public static int calculate(int price, int totalDays, int elapsedDays) {
        if (elapsedDays <= FREE_REFUND_DAY_LIMIT) {
            return price;
        }
        return Proration.calculate(price, totalDays, elapsedDays);
    }
}
