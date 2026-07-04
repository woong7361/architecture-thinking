package com.thinking.payment;

/**
 * 일할계산(PRORATION) 순수 도메인 로직.
 *
 * 사이클 2 Green: 두 예제(15000, 10000)가 하드코딩을 깨뜨려 일반식이 창발했다.
 * 일단가를 원 단위로 먼저 버림한 뒤(정수 나눗셈) 남은 일수를 곱한다.
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
