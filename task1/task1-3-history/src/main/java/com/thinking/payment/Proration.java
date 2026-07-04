package com.thinking.payment;

/**
 * 일할계산(PRORATION) 순수 도메인 로직.
 *
 * 사이클 1 Green: 예제가 하나뿐이라 삼각측량 규율에 따라 하드코딩으로 최소 통과시킨다.
 * 다음 사이클(30000/30/10 → 10000)이 이 하드코딩을 깨뜨릴 때 일반식으로 넘어간다.
 */
public final class Proration {

    private Proration() {
    }

    public static int calculate(int price, int totalDays, int elapsedDays) {
        return 15000;
    }
}
