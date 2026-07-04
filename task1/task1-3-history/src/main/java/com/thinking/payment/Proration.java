package com.thinking.payment;

/**
 * 일할계산(PRORATION) 순수 도메인 로직.
 *
 * calculate = 무료환불 정책(seam 바깥쪽) + prorate 순수 일할계산(seam 안쪽)의 조합 진입점.
 * 정책과 계산을 한 메서드에 접어두면 새 정책이 추가될 때마다 계산 로직까지 다시 읽어야 한다 —
 * 분리해두면 정책 변경은 calculate만, 계산 규칙 변경은 prorate만 건드리면 된다.
 */
public final class Proration {

    private static final int FREE_REFUND_DAY_LIMIT = 7;

    private Proration() {
    }

    public static int calculate(int price, int totalDays, int elapsedDays) {
        if (elapsedDays <= FREE_REFUND_DAY_LIMIT) {
            return price;
        }
        return prorate(price, totalDays, elapsedDays);
    }

    private static int prorate(int price, int totalDays, int elapsedDays) {
        int remaining = totalDays - elapsedDays;
        int dailyRate = price / totalDays; // 원 단위 버림 (정수 나눗셈)
        return dailyRate * remaining;
    }
}
