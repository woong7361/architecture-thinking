package com.thinking.payment;

/**
 * 일할계산(PRORATION) 순수 도메인 로직.
 *
 * 환불 정책(무료환불 기간 등)을 전혀 모른다 — 안다면 정책이 바뀔 때마다
 * 이 클래스도 바뀔 이유가 생겨 SRP를 어긴다. 정책은 {@link RefundCalculator}가 조합한다.
 */
public final class Proration {

    private Proration() {
    }

    public static int calculate(int price, int totalDays, int elapsedDays) {
        int remaining = totalDays - elapsedDays;
        int dailyRate = price / totalDays; // 원 단위 버림 (정수 나눗셈)
        return dailyRate * remaining;
    }
}
